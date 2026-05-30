package com.zavan.dedesite.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.zavan.dedesite.model.VisitorIdentity;
import com.zavan.dedesite.repository.VisitorIdentityRepository;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SiteStatusService {

    private static final String VISITOR_COOKIE = "dedesite_visitor";
    private static final int VISITOR_COOKIE_MAX_AGE = 60 * 60 * 24 * 365 * 2;
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);
    private static final int MAX_VISIBLE_VISITORS = 18;
    private static final Pattern BOT_USER_AGENT = Pattern.compile(
            "bot|crawler|spider|scan|slurp|curl|wget|python|httpclient|headless|preview|facebookexternalhit|discordbot|telegrambot|whatsapp",
            Pattern.CASE_INSENSITIVE);

    private final VisitorIdentityRepository visitorIdentityRepository;
    private final Instant startedAt = Instant.now();
    private final Instant lastUpdateAt = resolveLastUpdateAt();

    public SiteStatusService(VisitorIdentityRepository visitorIdentityRepository) {
        this.visitorIdentityRepository = visitorIdentityRepository;
    }

    public long getVisitorNumber(HttpServletRequest request, HttpServletResponse response) {
        if (isAutomatedRequest(request)) {
            return 0;
        }

        UUID visitorId = readVisitorId(request).orElseGet(() -> {
            UUID created = UUID.randomUUID();
            Cookie cookie = new Cookie(VISITOR_COOKIE, created.toString());
            cookie.setHttpOnly(true);
            cookie.setMaxAge(VISITOR_COOKIE_MAX_AGE);
            cookie.setPath("/");
            cookie.setSecure(request.isSecure());
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
            return created;
        });
        return findOrCreateVisitorNumber(visitorId);
    }

    public String getLastUpdateDistance() {
        Duration elapsed = Duration.between(lastUpdateAt, Instant.now());
        if (elapsed.isNegative()) {
            return "just now";
        }

        long days = elapsed.toDays();
        if (days > 0) {
            return plural(days, "day") + " ago";
        }

        long hours = elapsed.toHours();
        if (hours > 0) {
            return plural(hours, "hour") + " ago";
        }

        long minutes = elapsed.toMinutes();
        if (minutes > 0) {
            return plural(minutes, "minute") + " ago";
        }

        return "just now";
    }

    public List<OnlineVisitor> getOnlineVisitors(long currentVisitorNumber) {
        if (currentVisitorNumber == 0) {
            return List.of();
        }

        return visitorIdentityRepository.findAllByLastSeenAtAfterOrderByIdAsc(Instant.now().minus(ONLINE_WINDOW))
                .stream()
                .limit(MAX_VISIBLE_VISITORS)
                .map(identity -> new OnlineVisitor(identity.getId(), identity.getId() == currentVisitorNumber))
                .toList();
    }

    private Instant resolveLastUpdateAt() {
        return readEpochSecondsFromEnv("LAST_UPDATE_EPOCH_SECONDS")
                .or(() -> readEpochSecondsFromEnv("SOURCE_DATE_EPOCH"))
                .or(this::readLatestGitCommitTime)
                .or(this::readApplicationArtifactTime)
                .orElse(startedAt);
    }

    private Optional<Instant> readEpochSecondsFromEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Instant.ofEpochSecond(Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Instant> readLatestGitCommitTime() {
        try {
            Path repoRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
            Process process = new ProcessBuilder("git", "-C", repoRoot.toString(), "log", "-1", "--format=%ct")
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return Optional.empty();
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            return Optional.of(Instant.ofEpochSecond(Long.parseLong(output)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Instant> readApplicationArtifactTime() {
        try {
            URL location = SiteStatusService.class.getProtectionDomain().getCodeSource().getLocation();
            URI uri = location.toURI();
            File file = Path.of(uri).toFile();
            return Optional.of(Files.getLastModifiedTime(file.toPath()).toInstant());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String plural(long value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private Optional<UUID> readVisitorId(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (VISITOR_COOKIE.equals(cookie.getName())) {
                try {
                    return Optional.of(UUID.fromString(cookie.getValue()));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private boolean isAutomatedRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null || userAgent.isBlank() || BOT_USER_AGENT.matcher(userAgent).find();
    }

    private synchronized long findOrCreateVisitorNumber(UUID visitorId) {
        String visitorToken = visitorId.toString();
        return visitorIdentityRepository.findByVisitorToken(visitorToken)
                .map(identity -> {
                    identity.setLastSeenAt(Instant.now());
                    return visitorIdentityRepository.save(identity).getId();
                })
                .orElseGet(() -> {
                    VisitorIdentity identity = new VisitorIdentity();
                    identity.setVisitorToken(visitorToken);
                    return visitorIdentityRepository.save(identity).getId();
                });
    }

    public record OnlineVisitor(long number, boolean current) {
    }
}
