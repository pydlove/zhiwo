package com.example.blogger.entity;

import java.time.LocalDateTime;

public class ScheduledPush {
    private String id;
    private String pushTime;          // HH:mm 每日执行时间
    private Integer status;           // 0=待执行, 1=执行中, 2=已执行, 3=已取消
    private String lastExecutedDate;   // yyyy-MM-dd 上次执行日期（用于判断今日是否已执行）
    private String userFilterType;    // all / selected
    private String userIds;            // JSON array string
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPushTime() { return pushTime; }
    public void setPushTime(String pushTime) { this.pushTime = pushTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getLastExecutedDate() { return lastExecutedDate; }
    public void setLastExecutedDate(String lastExecutedDate) { this.lastExecutedDate = lastExecutedDate; }
    public String getUserFilterType() { return userFilterType; }
    public void setUserFilterType(String userFilterType) { this.userFilterType = userFilterType; }
    public String getUserIds() { return userIds; }
    public void setUserIds(String userIds) { this.userIds = userIds; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
