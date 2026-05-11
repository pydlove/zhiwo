package com.example.blogger.entity;

import java.time.LocalDateTime;

public class User {
    private String id;
    private String username;
    private String password;
    private Integer status;
    private String phone;
    private String email;
    private String wxId;
    private String wxName;
    private String nickName;
    private Integer aiLimit;
    private Integer trackLimit;
    private String platformLimit;
    private String avatar;
    private String template;
    private java.time.LocalDate expireDate;
    private java.time.LocalDateTime lastLogin;
    private String remark;
    private Integer canSetEmail;
    private Integer emailReceive;
    private String membershipPlanId;
    private String inviteCode;
    private String invitedBy;
    private Integer userType;
    private String adminId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String styleConfig;
    private java.util.List<String> trackIds;

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
    public String getWxName() { return wxName; }
    public void setWxName(String wxName) { this.wxName = wxName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
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
    public java.time.LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(java.time.LocalDate expireDate) { this.expireDate = expireDate; }
    public java.time.LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(java.time.LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getCanSetEmail() { return canSetEmail; }
    public void setCanSetEmail(Integer canSetEmail) { this.canSetEmail = canSetEmail; }
    public Integer getEmailReceive() { return emailReceive; }
    public void setEmailReceive(Integer emailReceive) { this.emailReceive = emailReceive; }
    public String getMembershipPlanId() { return membershipPlanId; }
    public void setMembershipPlanId(String membershipPlanId) { this.membershipPlanId = membershipPlanId; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }
    public Integer getUserType() { return userType; }
    public void setUserType(Integer userType) { this.userType = userType; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getStyleConfig() { return styleConfig; }
    public void setStyleConfig(String styleConfig) { this.styleConfig = styleConfig; }
    public java.util.List<String> getTrackIds() { return trackIds; }
    public void setTrackIds(java.util.List<String> trackIds) { this.trackIds = trackIds; }
}
