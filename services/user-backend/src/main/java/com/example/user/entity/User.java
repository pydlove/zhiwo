package com.example.user.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private String id;
    private String username;
    private String password;
    private Integer status;
    private String phone;
    private String email;
    private String wxId;
    private Integer aiLimit;
    private Integer trackLimit;
    private String platformLimit;
    private String avatar;
    private String template;
    private LocalDate expireDate;
    private LocalDateTime lastLogin;
    private String remark;
    private Integer canSetEmail;
    private Integer emailReceive;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWxId() { return wxId; }
    public void setWxId(String wxId) { this.wxId = wxId; }
    public Integer getAiLimit() { return aiLimit; }
    public void setAiLimit(Integer aiLimit) { this.aiLimit = aiLimit; }
    public Integer getTrackLimit() { return trackLimit; }
    public void setTrackLimit(Integer trackLimit) { this.trackLimit = trackLimit; }
    public String getPlatformLimit() { return platformLimit; }
    public void setPlatformLimit(String platformLimit) { this.platformLimit = platformLimit; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getCanSetEmail() { return canSetEmail; }
    public void setCanSetEmail(Integer canSetEmail) { this.canSetEmail = canSetEmail; }
    public Integer getEmailReceive() { return emailReceive; }
    public void setEmailReceive(Integer emailReceive) { this.emailReceive = emailReceive; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
