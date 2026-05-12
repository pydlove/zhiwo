package com.example.blogger.entity;

import java.time.LocalDateTime;

/**
 * 标题生成任务表
 * 用于本地-服务器联动生成文章
 */
public class TitleGenerationTask {
    private String id;
    private String titleLibraryId;  // 关联的标题库ID
    private String title;            // 标题内容
    private String prompt;           // 使用的提示词（已从模板填充变量）
    private String status;           // pending/processing/completed/failed
    private String resultFileUrl;    // 生成后服务器上的文件URL
    private String resultFileName;   // 生成的文件名
    private String errorMessage;     // 失败原因
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
    private Integer progressStep;
    private String progressMessage;
    private String generatedContent; // 大模型生成的原始内容

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitleLibraryId() { return titleLibraryId; }
    public void setTitleLibraryId(String titleLibraryId) { this.titleLibraryId = titleLibraryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResultFileUrl() { return resultFileUrl; }
    public void setResultFileUrl(String resultFileUrl) { this.resultFileUrl = resultFileUrl; }

    public String getResultFileName() { return resultFileName; }
    public void setResultFileName(String resultFileName) { this.resultFileName = resultFileName; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public Integer getProgressStep() { return progressStep; }
    public void setProgressStep(Integer progressStep) { this.progressStep = progressStep; }

    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }

    public String getGeneratedContent() { return generatedContent; }
    public void setGeneratedContent(String generatedContent) { this.generatedContent = generatedContent; }
}
