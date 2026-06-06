package com.horseracing.dto;

public class UserResponse {
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String roleName;
    private Boolean isActive;
    private Boolean isApproved;

    public UserResponse(Integer userId, String username, String fullName, String email, String phone,
                        String roleName, Boolean isActive, Boolean isApproved) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.roleName = roleName;
        this.isActive = isActive;
        this.isApproved = isApproved;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRoleName() {
        return roleName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }
}
