package com.zavan.dedesite.service;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.repository.CometRepository;
import com.zavan.dedesite.repository.OrbitRepository;
import com.zavan.dedesite.repository.StarRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ObservatoryService {
    private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:mm a");

    private final OrbitRepository orbitRepository;
    private final StarRepository starRepository;
    private final CometRepository cometRepository;

    public ObservatoryService(OrbitRepository orbitRepository, StarRepository starRepository, CometRepository cometRepository) {
        this.orbitRepository = orbitRepository;
        this.starRepository = starRepository;
        this.cometRepository = cometRepository;
    }

    public MissionControl missionControl(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        List<Orbit> todayOrbits = fixedOrbitsForDay(user, today.getDayOfWeek());
        List<Star> scheduledStars = starRepository.findByUserAndScheduledStartBetweenOrderByScheduledStartAsc(user, dayStart, dayEnd);
        List<Comet> todayComets = cometRepository.findByUserAndDateOrderByStartTimeAsc(user, today);
        List<Comet> upcomingComets = cometRepository.findByUserAndDateBetweenOrderByDateAscStartTimeAsc(user, today, today.plusDays(14));
        List<Orbit> activePulsars = orbitRepository.findByUserAndKindAndActiveTrueOrderByCreatedAtDesc(user, Orbit.Kind.PULSAR);
        List<Star> openStars = starRepository.findByUserAndStatusNotOrderByDueDateAscCreatedAtDesc(user, Star.Status.DONE);
        List<Star> nebulaStars = starRepository.findByUserAndStatusOrderByCreatedAtDesc(user, Star.Status.NEBULA);
        List<Star> supernovaStars = openStars.stream().filter(this::isSupernova).toList();
        List<Comet> supernovaComets = upcomingComets.stream().filter(this::isSupernova).toList();
        List<LaunchWindow> launchWindows = launchWindowsForDay(user, today);
        Star mainFocus = openStars.stream()
                .filter(star -> star.getStatus() == Star.Status.READY || star.getStatus() == Star.Status.SCHEDULED || star.getStatus() == Star.Status.IN_PROGRESS)
                .min(Comparator.comparing((Star star) -> star.getPriority().ordinal()).reversed()
                        .thenComparing(star -> star.getDueDate() == null ? LocalDate.MAX : star.getDueDate()))
                .orElse(openStars.isEmpty() ? null : openStars.getFirst());
        return new MissionControl(today, todayOrbits, scheduledStars, todayComets, upcomingComets,
                supernovaStars, supernovaComets, launchWindows, nebulaStars, activePulsars, mainFocus);
    }

    public SkyMap skyMap(User user) {
        return skyMap(user, LocalDate.now());
    }

    public SkyMap skyMap(User user, LocalDate anchorDate) {
        LocalDate weekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<SkyDay> days = new ArrayList<>();
        Duration totalLaunchWindow = Duration.ZERO;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<LaunchWindow> windows = launchWindowsForDay(user, day);
            totalLaunchWindow = totalLaunchWindow.plus(windows.stream()
                    .map(LaunchWindow::duration)
                    .reduce(Duration.ZERO, Duration::plus));
            days.add(new SkyDay(
                    day,
                    fixedOrbitsForDay(user, day.getDayOfWeek()),
                    starRepository.findByUserAndScheduledStartBetweenOrderByScheduledStartAsc(user, day.atStartOfDay(), day.plusDays(1).atStartOfDay()),
                    cometRepository.findByUserAndDateOrderByStartTimeAsc(user, day),
                    windows));
        }
        List<Star> weekStars = starRepository.findByUserAndScheduledStartBetweenOrderByScheduledStartAsc(user, weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay());
        Duration scheduledWork = weekStars.stream()
                .map(star -> durationBetween(star.getScheduledStart(), star.getScheduledEnd()))
                .reduce(Duration.ZERO, Duration::plus);
        boolean overloaded = scheduledWork.compareTo(totalLaunchWindow) > 0;
        return new SkyMap(weekStart, weekEnd, days, totalLaunchWindow, scheduledWork, overloaded);
    }

    public List<LaunchWindow> launchWindowsForDay(User user, LocalDate date) {
        List<BusyBlock> busy = new ArrayList<>();
        orbitRepository.findAllProjectedByUserAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(user, date.getDayOfWeek()).stream()
                .filter(OrbitRepository.TimedOrbit::isFixedBlock)
                .forEach(orbit -> {
            if (orbit.getStartTime() != null && orbit.getEndTime() != null) {
                busy.add(new BusyBlock(date.atTime(orbit.getStartTime()), date.atTime(orbit.getEndTime())));
            }
        });
        starRepository.findAllProjectedByUserAndScheduledStartBetweenOrderByScheduledStartAsc(user, date.atStartOfDay(), date.plusDays(1).atStartOfDay()).forEach(star -> {
            if (star.getScheduledStart() != null && star.getScheduledEnd() != null) {
                busy.add(new BusyBlock(star.getScheduledStart(), star.getScheduledEnd()));
            }
        });
        cometRepository.findAllProjectedByUserAndDateOrderByStartTimeAsc(user, date).forEach(comet -> {
            if (comet.getStartTime() != null && comet.getEndTime() != null) {
                busy.add(new BusyBlock(date.atTime(comet.getStartTime()), date.atTime(comet.getEndTime())));
            }
        });

        busy.sort(Comparator.comparing(BusyBlock::start));
        List<LaunchWindow> windows = new ArrayList<>();
        LocalDateTime cursor = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        for (BusyBlock block : busy) {
            LocalDateTime start = block.start().isBefore(cursor) ? cursor : block.start();
            if (start.isAfter(cursor) && Duration.between(cursor, start).toMinutes() >= 30) {
                windows.add(new LaunchWindow(cursor, start, recommendedEnergyType(cursor)));
            }
            if (block.end().isAfter(cursor)) {
                cursor = block.end();
            }
        }
        if (dayEnd.isAfter(cursor) && Duration.between(cursor, dayEnd).toMinutes() >= 30) {
            windows.add(new LaunchWindow(cursor, dayEnd, recommendedEnergyType(cursor)));
        }
        return windows;
    }

    public List<LaunchWindow> findLaunchWindows(User user, LocalDate startDate, LocalDate endDate) {
        List<LaunchWindow> windows = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            windows.addAll(launchWindowsForDay(user, cursor));
            cursor = cursor.plusDays(1);
        }
        return windows;
    }

    public List<TimelineBlock> timelineForDay(User user, LocalDate date) {
        List<TimelineBlock> blocks = new ArrayList<>();
        fixedOrbitsForDay(user, date.getDayOfWeek()).forEach(orbit -> {
            if (orbit.getStartTime() != null && orbit.getEndTime() != null) {
                blocks.add(new TimelineBlock(date.atTime(orbit.getStartTime()), date.atTime(orbit.getEndTime()), "Orbit", orbit.getTitle()));
            }
        });
        starRepository.findByUserAndScheduledStartBetweenOrderByScheduledStartAsc(user, date.atStartOfDay(), date.plusDays(1).atStartOfDay()).forEach(star -> {
            if (star.getScheduledStart() != null && star.getScheduledEnd() != null) {
                blocks.add(new TimelineBlock(star.getScheduledStart(), star.getScheduledEnd(), "Star", star.getTitle()));
            }
        });
        cometRepository.findByUserAndDateOrderByStartTimeAsc(user, date).forEach(comet -> {
            if (comet.getStartTime() != null && comet.getEndTime() != null) {
                blocks.add(new TimelineBlock(date.atTime(comet.getStartTime()), date.atTime(comet.getEndTime()), "Comet", comet.getTitle()));
            }
        });
        launchWindowsForDay(user, date).forEach(window ->
                blocks.add(new TimelineBlock(window.start(), window.end(), "Launch Window", "available")));
        blocks.sort(Comparator.comparing(TimelineBlock::start).thenComparing(TimelineBlock::end));
        return blocks;
    }

    public boolean isSupernova(Star star) {
        if (star.getPriority() == Star.Priority.CRITICAL) {
            return true;
        }
        if (star.isCompleted() || star.getDueDate() == null) {
            return false;
        }
        return !star.getDueDate().isAfter(LocalDate.now().plusDays(3));
    }

    public boolean isSupernova(Comet comet) {
        if (comet.getPriority() == Comet.Priority.CRITICAL) {
            return true;
        }
        return comet.getDate() != null && !comet.getDate().isAfter(LocalDate.now().plusDays(3));
    }

    public boolean useTwelveHourClock(String timeFormat) {
        return "12".equals(timeFormat);
    }

    public String oppositeTimeFormat(boolean twelveHourClock) {
        return twelveHourClock ? "24" : "12";
    }

    public String timeFormatLabel(boolean twelveHourClock) {
        return twelveHourClock ? "12h" : "24h";
    }

    public String formatTime(LocalTime time, boolean twelveHourClock) {
        if (time == null) {
            return "";
        }
        return time.format(twelveHourClock ? TIME_12 : TIME_24);
    }

    public String formatTime(LocalDateTime time, boolean twelveHourClock) {
        if (time == null) {
            return "";
        }
        return time.format(twelveHourClock ? TIME_12 : TIME_24);
    }

    public String formatRange(LocalTime start, LocalTime end, boolean twelveHourClock) {
        return formatTime(start, twelveHourClock) + " - " + formatTime(end, twelveHourClock);
    }

    public String formatRange(LocalDateTime start, LocalDateTime end, boolean twelveHourClock) {
        return formatTime(start, twelveHourClock) + " - " + formatTime(end, twelveHourClock);
    }

    public String formatLaunchWindow(LaunchWindow window, LocalDate day, boolean twelveHourClock) {
        String end = window.end().equals(day.plusDays(1).atStartOfDay())
                ? (twelveHourClock ? "next 12:00 AM" : "24:00")
                : formatTime(window.end(), twelveHourClock);
        return formatTime(window.start(), twelveHourClock) + " - " + end;
    }

    public String orbitColor(String colorKey) {
        return switch (colorKey == null ? "" : colorKey) {
            case "#b31312", "#f4eedf", "#b8b0a2", "#66717e", "#879c74", "#8d6b94",
                    "#4f7f86", "#d17a52", "#887f58", "#7d5f5f", "#576b8a", "#7f6d54",
                    "#5f765a", "#9b7250", "#6e627f", "#3f6e70" -> colorKey;
            default -> "#b8b0a2";
        };
    }

    private Duration durationBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return Duration.ZERO;
        }
        return Duration.between(start, end);
    }

    private List<Orbit> fixedOrbitsForDay(User user, DayOfWeek dayOfWeek) {
        return orbitRepository.findByUserAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(user, dayOfWeek).stream()
                .filter(Orbit::isFixedBlock)
                .toList();
    }

    private com.zavan.dedesite.model.StarSystem.EnergyType recommendedEnergyType(LocalDateTime start) {
        int hour = start.getHour();
        if (hour >= 21) {
            return com.zavan.dedesite.model.StarSystem.EnergyType.LIGHT_ADMIN;
        }
        if (hour >= 18) {
            return com.zavan.dedesite.model.StarSystem.EnergyType.CREATIVE;
        }
        return com.zavan.dedesite.model.StarSystem.EnergyType.DEEP_FOCUS;
    }

    private record BusyBlock(LocalDateTime start, LocalDateTime end) {}

    public record LaunchWindow(LocalDateTime start, LocalDateTime end, com.zavan.dedesite.model.StarSystem.EnergyType recommendedEnergyType) {
        public Duration duration() {
            return Duration.between(start, end);
        }

        public long minutes() {
            return duration().toMinutes();
        }
    }

    public record TimelineBlock(LocalDateTime start, LocalDateTime end, String type, String title) {}

    public record SkyDay(LocalDate date, List<Orbit> orbits, List<Star> stars, List<Comet> comets, List<LaunchWindow> launchWindows) {}

    public record SkyMap(LocalDate weekStart, LocalDate weekEnd, List<SkyDay> days, Duration availableTime, Duration scheduledWork, boolean overloaded) {
        public long availableHours() { return availableTime.toHours(); }
        public long scheduledHours() { return scheduledWork.toHours(); }
    }

    public record MissionControl(
            LocalDate date,
            List<Orbit> todayOrbits,
            List<Star> scheduledStars,
            List<Comet> todayComets,
            List<Comet> upcomingComets,
            List<Star> supernovaStars,
            List<Comet> supernovaComets,
            List<LaunchWindow> launchWindows,
            List<Star> nebulaStars,
            List<Orbit> activePulsars,
            Star mainFocus) {}
}
