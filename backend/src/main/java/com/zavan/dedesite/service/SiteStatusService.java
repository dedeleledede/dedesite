package com.zavan.dedesite.service;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class SiteStatusService {

    private static final String VISITOR_NUMBER = "visitorNumber";

    private final AtomicLong visitorCounter = new AtomicLong();
    private final Instant startedAt = Instant.now();
    private final Instant lastUpdateAt = resolveLastUpdateAt();

    public long getVisitorNumber(HttpSession session) {
        Object storedNumber = session.getAttribute(VISITOR_NUMBER);
        if (storedNumber instanceof Long number) {
            return number;
        }

        long nextNumber = visitorCounter.incrementAndGet();
        session.setAttribute(VISITOR_NUMBER, nextNumber);
        return nextNumber;
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
}
