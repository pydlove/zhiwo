package com.example.blogger.entity;

import java.time.LocalDateTime;

public class TitlePushLog {
    private String id;
    private String titleLibraryId;
    private String serverConfigId;
    private String title;
    private String platform;
    private String trackId;
    private String status;
    private String errorMsg;
    private String pushedBy;
    private LocalDateTime pushedAt;

    // display fields
    private String serverConfigName;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitleLibraryId() { return titleLibraryId; }
    public void setTitleLibraryId(String titleLibraryId) { this.titleLibraryId = titleLibraryId; }
    public String getServerConfigId() { return serverConfigId; }
    public void setServerConfigId(String serverConfigId) { this.serverConfigId = serverConfigId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getPushedBy() { return pushedBy; }
    public void setPushedBy(String pushedBy) { this.pushedBy = pushedBy; }
    public LocalDateTime getPushedAt() { return pushedAt; }
    public void setPushedAt(LocalDateTime pushedAt) { this.pushedAt = pushedAt; }
    public String getServerConfigName() { return serverConfigName; }
    public void setServerConfigName(String serverConfigName) { this.serverConfigName = serverConfigName; }
}
