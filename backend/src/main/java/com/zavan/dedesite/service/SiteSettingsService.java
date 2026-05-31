package com.zavan.dedesite.service;

import com.zavan.dedesite.model.SiteSettings;
import com.zavan.dedesite.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SiteSettingsService {
    private static final long SITE_SETTINGS_ID = 1L;

    private final SiteSettingsRepository repository;

    public SiteSettingsService(SiteSettingsRepository repository) {
        this.repository = repository;
    }

    public SiteSettings get() {
        SiteSettings settings = repository.findById(SITE_SETTINGS_ID).orElseGet(SiteSettings::new);
        if (settings.getSiteStatusNote() == null || settings.getSiteStatusNote().isBlank()) {
            settings.setSiteStatusNote("Running from theconstellation, deployed automatically from GitHub.");
        }
        if (settings.getNowBuildingLabel() == null || settings.getNowBuildingLabel().isBlank()) {
            settings.setNowBuildingLabel("now building");
        }
        if (settings.getHomeIntro() == null || settings.getHomeIntro().isBlank()) {
            settings.setHomeIntro("my corner of the internet for blog posts, projects, music notes, guestbook traces, and whatever else gets wired in next.");
        }
        if (settings.getSiteStatusTitle() == null || settings.getSiteStatusTitle().isBlank()) {
            settings.setSiteStatusTitle("site status");
        }
        if (settings.getLastFmEmptyText() == null || settings.getLastFmEmptyText().isBlank()) {
            settings.setLastFmEmptyText("no recent transmission.");
        }
        if (settings.getBroadcastTitle() == null || settings.getBroadcastTitle().isBlank()) {
            settings.setBroadcastTitle("broadcast");
        }
        if (settings.getBroadcastMessage() == null || settings.getBroadcastMessage().isBlank()) {
            settings.setBroadcastMessage("signal pending.");
        }
        return settings;
    }

    public SiteSettings save(SiteSettings form) {
        SiteSettings settings = get();
        settings.setNowBuildingTitle(form.getNowBuildingTitle());
        settings.setNowBuildingNote(form.getNowBuildingNote());
        settings.setNowBuildingLabel(form.getNowBuildingLabel());
        settings.setHomeIntro(form.getHomeIntro());
        settings.setSiteStatusTitle(form.getSiteStatusTitle());
        settings.setSiteStatusNote(form.getSiteStatusNote());
        settings.setLastFmEnabled(form.isLastFmEnabled());
        settings.setLastFmUsername(form.getLastFmUsername());
        if (!form.getLastFmApiKey().isBlank()) {
            settings.setLastFmApiKey(form.getLastFmApiKey());
        }
        settings.setLastFmEmptyText(form.getLastFmEmptyText());
        settings.setBroadcastEnabled(form.isBroadcastEnabled());
        settings.setBroadcastTitle(form.getBroadcastTitle());
        settings.setBroadcastMessage(form.getBroadcastMessage());
        return repository.save(settings);
    }
}
