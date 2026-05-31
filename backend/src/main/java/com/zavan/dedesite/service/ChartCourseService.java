package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.OrbitRepository;
import com.zavan.dedesite.repository.StarRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChartCourseService {
    private static final int MAX_SUGGESTED_MINUTES_PER_DAY = 180;
    private static final double FREE_TIME_FILL_RATIO = 0.65;
    private static final LocalTime AVOID_DEEP_FOCUS_AFTER = LocalTime.of(21, 0);
    private static final LocalTime PROTECT_WIND_DOWN_AFTER = LocalTime.of(22, 30);
    private static final LocalTime EARLIEST_SUGGESTION_TIME = LocalTime.of(6, 0);
    private static final int MINIMUM_BUFFER_MINUTES = 15;

    private final ObservatoryService observatoryService;
    private final OrbitRepository orbitRepository;
    private final StarRepository starRepository;
    private final StarService starService;
    private final PulsarService pulsarService;

    public ChartCourseService(ObservatoryService observatoryService,
                              OrbitRepository orbitRepository,
                              StarRepository starRepository,
                              StarService starService,
                              PulsarService pulsarService) {
        this.observatoryService = observatoryService;
        this.orbitRepository = orbitRepository;
        this.starRepository = starRepository;
        this.starService = starService;
        this.pulsarService = pulsarService;
    }

    public CoursePreview preview(User user, LocalDate anchorDate) {
        LocalDate weekStart = anchorDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<ObservatoryService.LaunchWindow> windows = observatoryService.findLaunchWindows(user, weekStart, weekEnd);
        List<PlanningCandidate> candidates = candidates(user);
        return new CoursePreview(weekStart, weekEnd, windows, fitCandidatesIntoWindows(candidates, windows));
    }

    public int accept(User user, LocalDate anchorDate, List<String> selectedKeys) {
        if (selectedKeys == null || selectedKeys.isEmpty()) {
            return 0;
        }
        Set<String> selected = selectedKeys.stream().collect(Collectors.toSet());
        int accepted = 0;
        for (ScheduleSuggestion suggestion : preview(user, anchorDate).suggestions()) {
            if (!suggestion.acceptable() || !selected.contains(suggestion.key())) {
                continue;
            }
            if (suggestion.sourceType() == PlanningCandidateType.STAR) {
                scheduleStar(user, suggestion);
            } else {
                createPulsarStar(user, suggestion);
            }
            accepted++;
        }
        return accepted;
    }

    private List<PlanningCandidate> candidates(User user) {
        List<PlanningCandidate> candidates = new ArrayList<>();
        starRepository.findByUserAndScheduledStartIsNullAndScheduledEndIsNullAndStatusNotOrderByDueDateAscCreatedAtAsc(user, Star.Status.DONE)
                .stream()
                .filter(star -> star.getStatus() == Star.Status.NEBULA || star.getStatus() == Star.Status.READY)
                .forEach(star -> {
                    int estimatedMinutes = Math.max(15, valueOrDefault(star.getEstimatedMinutes(), 60));
                    candidates.add(new PlanningCandidate(
                            star.getPublicId(),
                            PlanningCandidateType.STAR,
                            star.getTitle(),
                            star.getDescription(),
                            star.getPriority(),
                            energyOrDefault(star.getEnergyType()),
                            star.getDueDate() == null ? null : star.getDueDate().atTime(23, 59),
                            estimatedMinutes,
                            Math.min(estimatedMinutes, 30),
                            estimatedMinutes,
                            observatoryService.isSupernova(star),
                            true));
                });

        orbitRepository.findByUserAndKindAndActiveTrueOrderByCreatedAtDesc(user, Orbit.Kind.PULSAR)
                .stream()
                .filter(Orbit::isAutoSchedule)
                .forEach(pulsar -> candidates.addAll(splitPulsar(pulsar)));
        return candidates;
    }

    private List<PlanningCandidate> splitPulsar(Orbit pulsar) {
        List<PlanningCandidate> sessions = new ArrayList<>();
        int remaining = valueOrDefault(pulsar.getTargetMinutesPerWeek(), 0);
        int minimum = Math.max(15, valueOrDefault(pulsar.getMinimumSessionMinutes(), 30));
        int maximum = Math.max(minimum, valueOrDefault(pulsar.getMaximumSessionMinutes(), 90));
        while (remaining > 0) {
            int duration = Math.min(maximum, remaining);
            if (duration < minimum && !sessions.isEmpty()) {
                PlanningCandidate previous = sessions.removeLast();
                duration += previous.estimatedMinutes();
            }
            sessions.add(new PlanningCandidate(
                    pulsar.getPublicId(),
                    PlanningCandidateType.PULSAR_ORBIT,
                    pulsar.getTitle(),
                    pulsar.getDescription(),
                    pulsar.getPriority(),
                    energyOrDefault(pulsar.getEnergyType()),
                    null,
                    duration,
                    Math.min(minimum, duration),
                    Math.max(maximum, duration),
                    false,
                    pulsar.getStarSystem() != null));
            remaining -= Math.min(maximum, remaining);
        }
        return sessions;
    }

    private List<ScheduleSuggestion> fitCandidatesIntoWindows(
            List<PlanningCandidate> candidates,
            List<ObservatoryService.LaunchWindow> windows) {
        List<WindowCursor> cursors = windows.stream()
                .map(window -> new WindowCursor(window.start(), window.end()))
                .sorted(Comparator.comparing(WindowCursor::start))
                .toList();
        Map<LocalDate, Integer> freeMinutes = new HashMap<>();
        windows.forEach(window -> freeMinutes.merge(window.start().toLocalDate(), (int) window.minutes(), Integer::sum));
        Map<LocalDate, Integer> scheduledMinutes = new HashMap<>();
        List<ScheduleSuggestion> suggestions = new ArrayList<>();

        candidates.stream()
                .sorted(Comparator.comparingInt(this::scoreCandidate).reversed())
                .forEach(candidate -> placeCandidate(candidate, cursors, freeMinutes, scheduledMinutes, suggestions));
        return suggestions;
    }

    private void placeCandidate(PlanningCandidate candidate,
                                List<WindowCursor> windows,
                                Map<LocalDate, Integer> freeMinutes,
                                Map<LocalDate, Integer> scheduledMinutes,
                                List<ScheduleSuggestion> suggestions) {
        for (WindowCursor window : windows) {
            LocalDateTime start = normalizedSchedulingStart(window.cursor());
            LocalDate day = start.toLocalDate();
            int used = scheduledMinutes.getOrDefault(day, 0);
            int ratioLimit = (int) Math.floor(freeMinutes.getOrDefault(day, 0) * FREE_TIME_FILL_RATIO);
            int dailyRoom = Math.min(MAX_SUGGESTED_MINUTES_PER_DAY - used, ratioLimit - used);
            int available = (int) Duration.between(start, window.end()).toMinutes();
            int duration = Math.min(candidate.maximumSessionMinutes(), Math.min(candidate.estimatedMinutes(), Math.min(dailyRoom, available)));
            if (duration < candidate.minimumSessionMinutes() || !energyFits(candidate, start)) {
                continue;
            }
            LocalDateTime end = start.plusMinutes(duration);
            ScheduleSuggestion suggestion = new ScheduleSuggestion(
                    candidate.sourceId(),
                    candidate.sourceType(),
                    candidate.title(),
                    start,
                    end,
                    duration,
                    candidate.energyType(),
                    candidate.priority(),
                    reason(candidate),
                    candidate.acceptable());
            suggestions.add(suggestion);
            scheduledMinutes.merge(day, duration, Integer::sum);
            window.moveTo(end.plusMinutes(MINIMUM_BUFFER_MINUTES));
            return;
        }
    }

    private boolean energyFits(PlanningCandidate candidate, LocalDateTime start) {
        if (!start.toLocalTime().isBefore(PROTECT_WIND_DOWN_AFTER)) {
            return false;
        }
        return candidate.energyType() != StarSystem.EnergyType.DEEP_FOCUS
                || start.toLocalTime().isBefore(AVOID_DEEP_FOCUS_AFTER);
    }

    private LocalDateTime normalizedSchedulingStart(LocalDateTime start) {
        if (start.toLocalTime().isBefore(EARLIEST_SUGGESTION_TIME)) {
            return start.toLocalDate().atTime(EARLIEST_SUGGESTION_TIME);
        }
        return start;
    }

    private int scoreCandidate(PlanningCandidate candidate) {
        int score = candidate.supernova() ? 1000 : 0;
        score += switch (candidate.priority()) {
            case CRITICAL -> 500;
            case HIGH -> 250;
            case MEDIUM -> 100;
            case LOW -> 25;
        };
        if (candidate.dueAt() != null) {
            if (!candidate.dueAt().isAfter(LocalDateTime.now().plusDays(3))) {
                score += 200;
            } else if (!candidate.dueAt().isAfter(LocalDateTime.now().plusDays(7))) {
                score += 100;
            }
        }
        return score;
    }

    private String reason(PlanningCandidate candidate) {
        if (!candidate.acceptable()) {
            return "Link this Pulsar to a Star System before accepting.";
        }
        if (candidate.supernova()) {
            return "Supernova: urgent work placed first.";
        }
        if (candidate.sourceType() == PlanningCandidateType.PULSAR_ORBIT) {
            return "Pulsar target for this week.";
        }
        return "Fits available Launch Window.";
    }

    private void scheduleStar(User user, ScheduleSuggestion suggestion) {
        Star star = starService.getOwned(suggestion.sourceId(), user);
        if (star.getScheduledStart() != null || star.getScheduledEnd() != null || star.isCompleted()) {
            return;
        }
        star.setScheduledStart(suggestion.start());
        star.setScheduledEnd(suggestion.end());
        star.setStatus(Star.Status.SCHEDULED);
        starRepository.save(star);
    }

    private void createPulsarStar(User user, ScheduleSuggestion suggestion) {
        Orbit pulsar = pulsarService.getOwned(suggestion.sourceId(), user);
        if (pulsar.getStarSystem() == null) {
            return;
        }
        Star generated = new Star();
        generated.setUser(user);
        generated.setStarSystem(pulsar.getStarSystem());
        generated.setTitle("Pulsar: " + pulsar.getTitle());
        generated.setDescription(pulsar.getDescription());
        generated.setPriority(pulsar.getPriority());
        generated.setEnergyType(pulsar.getEnergyType());
        generated.setEstimatedMinutes(suggestion.durationMinutes());
        generated.setScheduledStart(suggestion.start());
        generated.setScheduledEnd(suggestion.end());
        generated.setStatus(Star.Status.SCHEDULED);
        starRepository.save(generated);
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private StarSystem.EnergyType energyOrDefault(StarSystem.EnergyType energyType) {
        return energyType == null ? StarSystem.EnergyType.LIGHT_ADMIN : energyType;
    }

    public enum PlanningCandidateType {
        STAR, PULSAR_ORBIT
    }

    public record PlanningCandidate(
            UUID sourceId,
            PlanningCandidateType sourceType,
            String title,
            String description,
            Star.Priority priority,
            StarSystem.EnergyType energyType,
            LocalDateTime dueAt,
            int estimatedMinutes,
            int minimumSessionMinutes,
            int maximumSessionMinutes,
            boolean supernova,
            boolean acceptable) {
    }

    public record ScheduleSuggestion(
            UUID sourceId,
            PlanningCandidateType sourceType,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            int durationMinutes,
            StarSystem.EnergyType energyType,
            Star.Priority priority,
            String reason,
            boolean acceptable) {
        public String key() {
            return sourceType + ":" + sourceId + ":" + start + ":" + end;
        }
    }

    public record CoursePreview(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<ObservatoryService.LaunchWindow> launchWindows,
            List<ScheduleSuggestion> suggestions) {
    }

    private static final class WindowCursor {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private LocalDateTime cursor;

        private WindowCursor(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
            this.cursor = start;
        }

        private LocalDateTime start() { return start; }
        private LocalDateTime end() { return end; }
        private LocalDateTime cursor() { return cursor; }
        private void moveTo(LocalDateTime next) { cursor = next; }
    }
}
