package com.example.blogger.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmailPushLog {
    private String id;
    private String userId;
    private LocalDate pushDate;
    private String type;
    private String titleLibraryId;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getPushDate() { return pushDate; }
    public void setPushDate(LocalDate pushDate) { this.pushDate = pushDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitleLibraryId() { return titleLibraryId; }
    public void setTitleLibraryId(String titleLibraryId) { this.titleLibraryId = titleLibraryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
