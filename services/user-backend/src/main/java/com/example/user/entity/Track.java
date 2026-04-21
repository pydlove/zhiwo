package com.example.user.entity;

import java.time.LocalDateTime;

public class Track {
    private String id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private String previewBloggers;
    private String intro;
    private String platforms;
    private String coverJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    // display fields
    private Integer bloggerCount;
    private Integer postCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getPreviewBloggers() { return previewBloggers; }
    public void setPreviewBloggers(String previewBloggers) { this.previewBloggers = previewBloggers; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }
    public String getCoverJson() { return coverJson; }
    public void setCoverJson(String coverJson) { this.coverJson = coverJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getBloggerCount() { return bloggerCount; }
    public void setBloggerCount(Integer bloggerCount) { this.bloggerCount = bloggerCount; }
    public Integer getPostCount() { return postCount; }
    public void setPostCount(Integer postCount) { this.postCount = postCount; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
