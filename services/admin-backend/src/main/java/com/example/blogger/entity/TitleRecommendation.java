package com.example.blogger.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TitleRecommendation {
    private String id;
    private String titleLibraryId;
    private String userId;
    private String platform;
    private String trackId;
    private LocalDate recommendDate;
    private LocalDateTime createdAt;

    private String subscriptionPostId;

    // display fields
    private String userName;
    private String userTemplate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitleLibraryId() { return titleLibraryId; }
    public void setTitleLibraryId(String titleLibraryId) { this.titleLibraryId = titleLibraryId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public LocalDate getRecommendDate() { return recommendDate; }
    public void setRecommendDate(LocalDate recommendDate) { this.recommendDate = recommendDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserTemplate() { return userTemplate; }
    public void setUserTemplate(String userTemplate) { this.userTemplate = userTemplate; }
    public String getSubscriptionPostId() { return subscriptionPostId; }
    public void setSubscriptionPostId(String subscriptionPostId) { this.subscriptionPostId = subscriptionPostId; }
}
