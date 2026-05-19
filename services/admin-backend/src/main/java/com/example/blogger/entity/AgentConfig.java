package com.example.blogger.entity;

import java.time.LocalDateTime;

public class AgentConfig {
    private Long id;
    private Integer enabled;
    private String cronExpr;
    private Double similarityThreshold;
    private Double homogeneityThreshold;
    private Integer minTitlesPerTrack;
    private Integer historyDays;
    private Integer candidateLimit;
    private Integer maxGenerationConcurrency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }

    public String getCronExpr() { return cronExpr; }
    public void setCronExpr(String cronExpr) { this.cronExpr = cronExpr; }

    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public Double getHomogeneityThreshold() { return homogeneityThreshold; }
    public void setHomogeneityThreshold(Double homogeneityThreshold) { this.homogeneityThreshold = homogeneityThreshold; }

    public Integer getMinTitlesPerTrack() { return minTitlesPerTrack; }
    public void setMinTitlesPerTrack(Integer minTitlesPerTrack) { this.minTitlesPerTrack = minTitlesPerTrack; }

    public Integer getHistoryDays() { return historyDays; }
    public void setHistoryDays(Integer historyDays) { this.historyDays = historyDays; }

    public Integer getCandidateLimit() { return candidateLimit; }
    public void setCandidateLimit(Integer candidateLimit) { this.candidateLimit = candidateLimit; }

    public Integer getMaxGenerationConcurrency() { return maxGenerationConcurrency; }
    public void setMaxGenerationConcurrency(Integer maxGenerationConcurrency) { this.maxGenerationConcurrency = maxGenerationConcurrency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
