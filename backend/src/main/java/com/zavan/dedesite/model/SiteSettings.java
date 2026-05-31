package com.zavan.dedesite.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "site_settings")
public class SiteSettings {
    @Id
    private Long id = 1L;

    @NotBlank
    @Size(max = 160)
    private String nowBuildingTitle = "dedesite / S&S / star_byte";

    @NotBlank
    @Size(max = 320)
    private String nowBuildingNote = "projects page rewired with current work and source links.";

    @Size(min = 1, max = 80)
    private String nowBuildingLabel = "now building";

    @Size(min = 1, max = 480)
    private String homeIntro = "my corner of the internet for blog posts, projects, music notes, guestbook traces, and whatever else gets wired in next.";

    @Size(min = 1, max = 80)
    private String siteStatusTitle = "site status";

    @NotBlank
    @Size(max = 320)
    private String siteStatusNote = "Running from theconstellation, deployed automatically from GitHub.";

    private boolean lastFmEnabled;

    @Size(max = 120)
    private String lastFmUsername = "";

    @Column(name = "encrypted_lastfm_api_key", length = 1024)
    @Convert(converter = EncryptedStringConverter.class)
    private String lastFmApiKey = "";

    @Size(min = 1, max = 160)
    private String lastFmEmptyText = "no recent transmission.";

    private boolean broadcastEnabled = true;

    @Size(min = 1, max = 80)
    private String broadcastTitle = "broadcast";

    @Size(min = 1, max = 320)
    private String broadcastMessage = "signal pending.";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNowBuildingTitle() { return nowBuildingTitle; }
    public void setNowBuildingTitle(String nowBuildingTitle) { this.nowBuildingTitle = nowBuildingTitle; }
    public String getNowBuildingNote() { return nowBuildingNote; }
    public void setNowBuildingNote(String nowBuildingNote) { this.nowBuildingNote = nowBuildingNote; }
    public String getNowBuildingLabel() { return nowBuildingLabel; }
    public void setNowBuildingLabel(String nowBuildingLabel) { this.nowBuildingLabel = nowBuildingLabel; }
    public String getHomeIntro() { return homeIntro; }
    public void setHomeIntro(String homeIntro) { this.homeIntro = homeIntro; }
    public String getSiteStatusTitle() { return siteStatusTitle; }
    public void setSiteStatusTitle(String siteStatusTitle) { this.siteStatusTitle = siteStatusTitle; }
    public String getSiteStatusNote() { return siteStatusNote; }
    public void setSiteStatusNote(String siteStatusNote) { this.siteStatusNote = siteStatusNote; }
    public boolean isLastFmEnabled() { return lastFmEnabled; }
    public void setLastFmEnabled(boolean lastFmEnabled) { this.lastFmEnabled = lastFmEnabled; }
    public String getLastFmUsername() { return lastFmUsername == null ? "" : lastFmUsername; }
    public void setLastFmUsername(String lastFmUsername) { this.lastFmUsername = lastFmUsername; }
    public String getLastFmApiKey() { return lastFmApiKey == null ? "" : lastFmApiKey; }
    public void setLastFmApiKey(String lastFmApiKey) { this.lastFmApiKey = lastFmApiKey; }
    public String getLastFmEmptyText() { return lastFmEmptyText; }
    public void setLastFmEmptyText(String lastFmEmptyText) { this.lastFmEmptyText = lastFmEmptyText; }
    public boolean isBroadcastEnabled() { return broadcastEnabled; }
    public void setBroadcastEnabled(boolean broadcastEnabled) { this.broadcastEnabled = broadcastEnabled; }
    public String getBroadcastTitle() { return broadcastTitle; }
    public void setBroadcastTitle(String broadcastTitle) { this.broadcastTitle = broadcastTitle; }
    public String getBroadcastMessage() { return broadcastMessage; }
    public void setBroadcastMessage(String broadcastMessage) { this.broadcastMessage = broadcastMessage; }
}
