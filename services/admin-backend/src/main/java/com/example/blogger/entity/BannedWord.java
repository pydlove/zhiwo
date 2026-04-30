package com.example.blogger.entity;

import java.time.LocalDateTime;

public class BannedWord {
    private String id;
    private String word;
    private String replacement;
    private String category;
    private String severity;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getReplacement() { return replacement; }
    public void setReplacement(String replacement) { this.replacement = replacement; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
