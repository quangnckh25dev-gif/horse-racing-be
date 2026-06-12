package com.horseracing.dto;

public class RefereeResponse {

    private Integer refereeId;
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String badgeNumber;
    private String speciality;

    public RefereeResponse(Integer refereeId, Integer userId, String username, String fullName,
                           String email, String badgeNumber, String speciality) {
        this.refereeId = refereeId;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.badgeNumber = badgeNumber;
        this.speciality = speciality;
    }

    public Integer getRefereeId() {
        return refereeId;
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

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public String getSpeciality() {
        return speciality;
    }
}
