package com.example.blogger.entity;

import java.time.LocalDateTime;

public class ProcessAutoConfig {
    private String id;
    private String checkTime;
    private String checkPlatforms;
    private Integer checkAllTracks;
    private Integer autoNotifyLocal;
    private Integer titlesPerTrack;
    private Integer autoPushAfterApprove;
    private Integer autoMatchAfterPush;
    private Integer isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCheckTime() { return checkTime; }
    public void setCheckTime(String checkTime) { this.checkTime = checkTime; }

    public String getCheckPlatforms() { return checkPlatforms; }
    public void setCheckPlatforms(String checkPlatforms) { this.checkPlatforms = checkPlatforms; }

    public Integer getCheckAllTracks() { return checkAllTracks; }
    public void setCheckAllTracks(Integer checkAllTracks) { this.checkAllTracks = checkAllTracks; }

    public Integer getAutoNotifyLocal() { return autoNotifyLocal; }
    public void setAutoNotifyLocal(Integer autoNotifyLocal) { this.autoNotifyLocal = autoNotifyLocal; }

    public Integer getTitlesPerTrack() { return titlesPerTrack; }
    public void setTitlesPerTrack(Integer titlesPerTrack) { this.titlesPerTrack = titlesPerTrack; }

    public Integer getAutoPushAfterApprove() { return autoPushAfterApprove; }
    public void setAutoPushAfterApprove(Integer autoPushAfterApprove) { this.autoPushAfterApprove = autoPushAfterApprove; }

    public Integer getAutoMatchAfterPush() { return autoMatchAfterPush; }
    public void setAutoMatchAfterPush(Integer autoMatchAfterPush) { this.autoMatchAfterPush = autoMatchAfterPush; }

    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
