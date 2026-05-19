package com.example.blogger.entity;

import java.time.LocalDateTime;

public class WritingStyle {
    private String id;
    private String originalWord;
    private String styleWord;
    private String category;
    private Integer isActive;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalWord() { return originalWord; }
    public void setOriginalWord(String originalWord) { this.originalWord = originalWord; }

    public String getStyleWord() { return styleWord; }
    public void setStyleWord(String styleWord) { this.styleWord = styleWord; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
