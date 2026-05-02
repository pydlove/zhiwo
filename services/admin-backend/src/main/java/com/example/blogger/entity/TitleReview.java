package com.example.blogger.entity;

import java.time.LocalDateTime;

public class TitleReview {
    private String id;
    private String titleLibraryId;
    private String reviewStatus;
    private String reviewReason;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String pushStatus;
    private LocalDateTime pushedAt;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // display fields
    private String title;
    private String description;
    private String platform;
    private String trackId;
    private String trackName;
    private Integer useCount;
    private Integer isUsed;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitleLibraryId() { return titleLibraryId; }
    public void setTitleLibraryId(String titleLibraryId) { this.titleLibraryId = titleLibraryId; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getPushStatus() { return pushStatus; }
    public void setPushStatus(String pushStatus) { this.pushStatus = pushStatus; }
    public LocalDateTime getPushedAt() { return pushedAt; }
    public void setPushedAt(LocalDateTime pushedAt) { this.pushedAt = pushedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }
    public Integer getUseCount() { return useCount; }
    public void setUseCount(Integer useCount) { this.useCount = useCount; }
    public Integer getIsUsed() { return isUsed; }
    public void setIsUsed(Integer isUsed) { this.isUsed = isUsed; }
}
