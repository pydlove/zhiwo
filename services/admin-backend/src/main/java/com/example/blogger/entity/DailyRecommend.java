package com.example.blogger.entity;

import java.time.LocalDateTime;

public class DailyRecommend {
    private String id;
    private String trackId;
    private String platform;
    private String title;
    private String summary;
    private String refPostId;
    private String refUrl;
    private String refPostTitle;
    private Integer sortOrder;
    private String status;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRefPostId() { return refPostId; }
    public void setRefPostId(String refPostId) { this.refPostId = refPostId; }
    public String getRefUrl() { return refUrl; }
    public void setRefUrl(String refUrl) { this.refUrl = refUrl; }
    public String getRefPostTitle() { return refPostTitle; }
    public void setRefPostTitle(String refPostTitle) { this.refPostTitle = refPostTitle; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
