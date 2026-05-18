package com.example.blogger.entity;

import java.time.LocalDateTime;

public class UserHomogeneity {
    private String id;
    private String userId;
    private Integer homogeneityScore;
    private Integer historyCount;
    private LocalDateTime calculatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getHomogeneityScore() { return homogeneityScore; }
    public void setHomogeneityScore(Integer homogeneityScore) { this.homogeneityScore = homogeneityScore; }

    public Integer getHistoryCount() { return historyCount; }
    public void setHistoryCount(Integer historyCount) { this.historyCount = historyCount; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
