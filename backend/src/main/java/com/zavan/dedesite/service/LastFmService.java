package com.zavan.dedesite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LastFmService {
    private static final Duration CACHE_DURATION = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final SiteSettingsService siteSettingsService;
    private Optional<Track> cachedTrack = Optional.empty();
    private Instant cacheExpiresAt = Instant.EPOCH;
    private String cachedSettingsKey = "";

    public LastFmService(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
        this.restClient = RestClient.create("https://ws.audioscrobbler.com");
    }

    public synchronized Optional<Track> getLatestTrack() {
        var settings = siteSettingsService.get();
        String username = settings.getLastFmUsername();
        String apiKey = settings.getLastFmApiKey();
        String settingsKey = settings.isLastFmEnabled() + ":" + username + ":" + apiKey;
        if (!settingsKey.equals(cachedSettingsKey)) {
            cachedSettingsKey = settingsKey;
            cacheExpiresAt = Instant.EPOCH;
        }
        if (!settings.isLastFmEnabled() || username.isBlank() || apiKey.isBlank()) {
            return Optional.empty();
        }
        if (Instant.now().isBefore(cacheExpiresAt)) {
            return cachedTrack;
        }

        try {
            Map<?, ?> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/2.0/")
                            .queryParam("method", "user.getrecenttracks")
                            .queryParam("user", username)
                            .queryParam("api_key", apiKey)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(Map.class);
            cachedTrack = readTrack(body);
        } catch (RuntimeException ignored) {
            cachedTrack = Optional.empty();
        }
        cacheExpiresAt = Instant.now().plus(CACHE_DURATION);
        return cachedTrack;
    }

    private Optional<Track> readTrack(Map<?, ?> body) {
        Object recentTracks = body == null ? null : body.get("recenttracks");
        if (!(recentTracks instanceof Map<?, ?> recentTracksMap)) {
            return Optional.empty();
        }
        Object rawTracks = recentTracksMap.get("track");
        if (!(rawTracks instanceof List<?> tracks) || tracks.isEmpty() || !(tracks.getFirst() instanceof Map<?, ?> track)) {
            return Optional.empty();
        }

        String title = text(track.get("name"));
        String artist = nestedText(track.get("artist"), "#text");
        String album = nestedText(track.get("album"), "#text");
        boolean nowPlaying = "true".equalsIgnoreCase(nestedText(track.get("@attr"), "nowplaying"));
        if (title.isBlank() || artist.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Track(title, artist, album, nowPlaying));
    }

    private String nestedText(Object value, String key) {
        return value instanceof Map<?, ?> map ? text(map.get(key)) : "";
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    public record Track(String title, String artist, String album, boolean nowPlaying) {}
}
