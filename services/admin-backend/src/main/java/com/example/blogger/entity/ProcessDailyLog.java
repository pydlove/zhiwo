package com.example.blogger.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProcessDailyLog {
    private String id;
    private LocalDate targetDate;
    private LocalDateTime checkTime;
    private String status;
    private Integer titlesNeeded;
    private Integer titlesGenerated;
    private Integer titlesApproved;
    private Integer titlesPushed;
    private Integer titlesMatched;
    private Integer articlesNeeded;
    private Integer articlesUploaded;
    private LocalDateTime pushScheduledTime;
    private Integer pushSuccess;
    private Integer pushFailed;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTitlesNeeded() { return titlesNeeded; }
    public void setTitlesNeeded(Integer titlesNeeded) { this.titlesNeeded = titlesNeeded; }

    public Integer getTitlesGenerated() { return titlesGenerated; }
    public void setTitlesGenerated(Integer titlesGenerated) { this.titlesGenerated = titlesGenerated; }

    public Integer getTitlesApproved() { return titlesApproved; }
    public void setTitlesApproved(Integer titlesApproved) { this.titlesApproved = titlesApproved; }

    public Integer getTitlesPushed() { return titlesPushed; }
    public void setTitlesPushed(Integer titlesPushed) { this.titlesPushed = titlesPushed; }

    public Integer getTitlesMatched() { return titlesMatched; }
    public void setTitlesMatched(Integer titlesMatched) { this.titlesMatched = titlesMatched; }

    public Integer getArticlesNeeded() { return articlesNeeded; }
    public void setArticlesNeeded(Integer articlesNeeded) { this.articlesNeeded = articlesNeeded; }

    public Integer getArticlesUploaded() { return articlesUploaded; }
    public void setArticlesUploaded(Integer articlesUploaded) { this.articlesUploaded = articlesUploaded; }

    public LocalDateTime getPushScheduledTime() { return pushScheduledTime; }
    public void setPushScheduledTime(LocalDateTime pushScheduledTime) { this.pushScheduledTime = pushScheduledTime; }

    public Integer getPushSuccess() { return pushSuccess; }
    public void setPushSuccess(Integer pushSuccess) { this.pushSuccess = pushSuccess; }

    public Integer getPushFailed() { return pushFailed; }
    public void setPushFailed(Integer pushFailed) { this.pushFailed = pushFailed; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
