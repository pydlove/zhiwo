package com.example.blogger.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MembershipPlan {
    private String id;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String featuresJson;
    private Integer trackLimit;
    private Integer aiLimit;
    private String platformLimit;
    private Integer expireDays;
    private String permissionsJson;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
    public Integer getTrackLimit() { return trackLimit; }
    public void setTrackLimit(Integer trackLimit) { this.trackLimit = trackLimit; }
    public Integer getAiLimit() { return aiLimit; }
    public void setAiLimit(Integer aiLimit) { this.aiLimit = aiLimit; }
    public String getPlatformLimit() { return platformLimit; }
    public void setPlatformLimit(String platformLimit) { this.platformLimit = platformLimit; }
    public Integer getExpireDays() { return expireDays; }
    public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
    public String getPermissionsJson() { return permissionsJson; }
    public void setPermissionsJson(String permissionsJson) { this.permissionsJson = permissionsJson; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
