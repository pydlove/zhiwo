package com.example.blogger.entity;

import java.time.LocalDateTime;

public class Post {
    private String id;
    private String title;
    private String url;
    private String bloggerId;
    private String content;
    private String platform;
    private String summary;
    private String tag;
    private String reads;
    private String likes;
    private String comments;
    private String metricsJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getBloggerId() { return bloggerId; }
    public void setBloggerId(String bloggerId) { this.bloggerId = bloggerId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getReads() { return reads; }
    public void setReads(String reads) { this.reads = reads; }
    public String getLikes() { return likes; }
    public void setLikes(String likes) { this.likes = likes; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
