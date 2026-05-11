package com.example.blogger.entity;

import java.time.LocalDateTime;

public class AiFlavorRule {
    private String id;
    private String ruleFrom;
    private String ruleTo;
    private Integer sortOrder;
    private Integer isEnabled;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuleFrom() { return ruleFrom; }
    public void setRuleFrom(String ruleFrom) { this.ruleFrom = ruleFrom; }

    public String getRuleTo() { return ruleTo; }
    public void setRuleTo(String ruleTo) { this.ruleTo = ruleTo; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}