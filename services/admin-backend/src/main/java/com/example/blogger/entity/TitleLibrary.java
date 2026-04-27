package com.example.blogger.entity;

import java.time.LocalDateTime;

public class TitleLibrary {
    private String id;
    private String title;
    private String description;
    private java.time.LocalDate pushDate;
    private String platform;
    private String trackId;
    private Integer useCount;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // display fields
    private String trackName;

    // recommendation display fields
    private String recommendUserId;
    private String recommendUserName;
    private String recommendUserTemplate;
    private java.time.LocalDate recommendDate;

    // subscription post display fields
    private String subscriptionPostId;
    private String subscriptionPostTitle;
    private String subscriptionPostFileUrl;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.time.LocalDate getPushDate() { return pushDate; }
    public void setPushDate(java.time.LocalDate pushDate) { this.pushDate = pushDate; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public Integer getUseCount() { return useCount; }
    public void setUseCount(Integer useCount) { this.useCount = useCount; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }
    public String getRecommendUserId() { return recommendUserId; }
    public void setRecommendUserId(String recommendUserId) { this.recommendUserId = recommendUserId; }
    public String getRecommendUserName() { return recommendUserName; }
    public void setRecommendUserName(String recommendUserName) { this.recommendUserName = recommendUserName; }
    public String getRecommendUserTemplate() { return recommendUserTemplate; }
    public void setRecommendUserTemplate(String recommendUserTemplate) { this.recommendUserTemplate = recommendUserTemplate; }
    public java.time.LocalDate getRecommendDate() { return recommendDate; }
    public void setRecommendDate(java.time.LocalDate recommendDate) { this.recommendDate = recommendDate; }
    public String getSubscriptionPostId() { return subscriptionPostId; }
    public void setSubscriptionPostId(String subscriptionPostId) { this.subscriptionPostId = subscriptionPostId; }
    public String getSubscriptionPostTitle() { return subscriptionPostTitle; }
    public void setSubscriptionPostTitle(String subscriptionPostTitle) { this.subscriptionPostTitle = subscriptionPostTitle; }
    public String getSubscriptionPostFileUrl() { return subscriptionPostFileUrl; }
    public void setSubscriptionPostFileUrl(String subscriptionPostFileUrl) { this.subscriptionPostFileUrl = subscriptionPostFileUrl; }
}
