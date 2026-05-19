package com.example.blogger.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AgentExecution {
    private Long id;
    private LocalDate executionDate;
    private String status;
    private Integer totalUsers;
    private Integer totalTracks;
    private Integer matchedTitles;
    private Integer generatedTitles;
    private Integer articleTasks;
    private Integer failedCount;
    private String detailJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }

    public Integer getTotalTracks() { return totalTracks; }
    public void setTotalTracks(Integer totalTracks) { this.totalTracks = totalTracks; }

    public Integer getMatchedTitles() { return matchedTitles; }
    public void setMatchedTitles(Integer matchedTitles) { this.matchedTitles = matchedTitles; }

    public Integer getGeneratedTitles() { return generatedTitles; }
    public void setGeneratedTitles(Integer generatedTitles) { this.generatedTitles = generatedTitles; }

    public Integer getArticleTasks() { return articleTasks; }
    public void setArticleTasks(Integer articleTasks) { this.articleTasks = articleTasks; }

    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
