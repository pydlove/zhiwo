package com.example.blogger.entity;

import java.time.LocalDateTime;

/**
 * V2 标题生成任务表
 * 基于大模型（kimi/minimax）异步生成标题
 */
public class TitleGenerateTask {
    private String id;
    private String status;           // pending/processing/completed/failed/stopped
    private String platforms;        // JSON 数组字符串
    private String trackIds;         // JSON 数组字符串
    private Integer countPerCombo;   // 每个组合生成数量
    private String instruction;      // 生成方向
    private String resultFileUrl;    // 生成结果 Excel 文件 URL
    private String resultFileName;   // 生成结果 Excel 文件名
    private String errorMessage;     // 失败原因
    private Integer progressStep;    // 进度步骤
    private String progressMessage;  // 当前进度描述
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }

    public String getTrackIds() { return trackIds; }
    public void setTrackIds(String trackIds) { this.trackIds = trackIds; }

    public Integer getCountPerCombo() { return countPerCombo; }
    public void setCountPerCombo(Integer countPerCombo) { this.countPerCombo = countPerCombo; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getResultFileUrl() { return resultFileUrl; }
    public void setResultFileUrl(String resultFileUrl) { this.resultFileUrl = resultFileUrl; }

    public String getResultFileName() { return resultFileName; }
    public void setResultFileName(String resultFileName) { this.resultFileName = resultFileName; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getProgressStep() { return progressStep; }
    public void setProgressStep(Integer progressStep) { this.progressStep = progressStep; }

    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
