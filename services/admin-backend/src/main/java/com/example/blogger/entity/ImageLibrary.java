package com.example.blogger.entity;

import java.time.LocalDateTime;

public class ImageLibrary {
    private String id;
    private String name;
    private String url;
    private String categories; // JSON array of track ids, e.g. ["1","2"]
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
