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
    private Integer isUsed;
    private Integer isDeleted;
    private Integer aiFlavorStatus; // 0/null=未检测 1=已通过 2=AI味重
    private Integer isCopied;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String generatedFileUrl;
    private String generatedFileName;
    private LocalDateTime generatedAt;
    private Integer generateStatus; // 0=未生成 1=生成成功
    private Integer isConfirmed; // 0=未确认 1=已确认 (兼容旧字段)
    private Integer confirmStatus; // 0=未确认 1=已确认 2=已拒绝
    private String imagePostUrls; // JSON 数组字符串，存储贴图 URL 列表
    private String titleKeyword;   // 标题分词关键词，用于相似度检测

    // display fields
    private String taskId;
    private String trackName;
    private String bannedWordCheckResult;

    // recommendation display fields
    private String recommendUserId;
    private String recommendUserName;
    private String recommendUserTemplate;
    private java.time.LocalDate recommendDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.time.LocalDate getPushDate() { return recommendDate != null ? recommendDate : pushDate; }
    public void setPushDate(java.time.LocalDate pushDate) { this.pushDate = pushDate; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public Integer getUseCount() { return useCount; }
    public void setUseCount(Integer useCount) { this.useCount = useCount; }
    public Integer getIsUsed() { return isUsed; }
    public void setIsUsed(Integer isUsed) { this.isUsed = isUsed; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public Integer getAiFlavorStatus() { return aiFlavorStatus; }
    public void setAiFlavorStatus(Integer aiFlavorStatus) { this.aiFlavorStatus = aiFlavorStatus; }
    public Integer getIsCopied() { return isCopied; }
    public void setIsCopied(Integer isCopied) { this.isCopied = isCopied; }
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
    public String getGeneratedFileUrl() { return generatedFileUrl; }
    public void setGeneratedFileUrl(String generatedFileUrl) { this.generatedFileUrl = generatedFileUrl; }
    public String getGeneratedFileName() { return generatedFileName; }
    public void setGeneratedFileName(String generatedFileName) { this.generatedFileName = generatedFileName; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public Integer getGenerateStatus() { return generateStatus; }
    public void setGenerateStatus(Integer generateStatus) { this.generateStatus = generateStatus; }
    public Integer getIsConfirmed() { return isConfirmed; }
    public void setIsConfirmed(Integer isConfirmed) { this.isConfirmed = isConfirmed; }
    public Integer getConfirmStatus() { return confirmStatus; }
    public void setConfirmStatus(Integer confirmStatus) { this.confirmStatus = confirmStatus; }
    public String getImagePostUrls() { return imagePostUrls; }
    public void setImagePostUrls(String imagePostUrls) { this.imagePostUrls = imagePostUrls; }
    public String getTitleKeyword() { return titleKeyword; }
    public void setTitleKeyword(String titleKeyword) { this.titleKeyword = titleKeyword; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getBannedWordCheckResult() { return bannedWordCheckResult; }
    public void setBannedWordCheckResult(String bannedWordCheckResult) { this.bannedWordCheckResult = bannedWordCheckResult; }
}
