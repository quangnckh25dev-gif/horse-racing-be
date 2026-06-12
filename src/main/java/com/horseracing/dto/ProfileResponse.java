package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfileResponse {
    private String profileType;
    private Integer profileId;
    private Integer userId;
    private String username;
    private String fullName;
    private String nationalId;
    private String address;
    private String organization;
    private String licenseNumber;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private Integer experienceYear;
    private Integer totalRaces;
    private Integer totalWins;
    private String badgeNumber;
    private String speciality;
    private LocalDateTime createdAt;

    public ProfileResponse(String profileType, Integer profileId, Integer userId, String username, String fullName,
                           String nationalId, String address, String organization, String licenseNumber,
                           BigDecimal weightKg, BigDecimal heightCm, Integer experienceYear, Integer totalRaces,
                           Integer totalWins, String badgeNumber, String speciality, LocalDateTime createdAt) {
        this.profileType = profileType;
        this.profileId = profileId;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.nationalId = nationalId;
        this.address = address;
        this.organization = organization;
        this.licenseNumber = licenseNumber;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.experienceYear = experienceYear;
        this.totalRaces = totalRaces;
        this.totalWins = totalWins;
        this.badgeNumber = badgeNumber;
        this.speciality = speciality;
        this.createdAt = createdAt;
    }

    public String getProfileType() { return profileType; }
    public Integer getProfileId() { return profileId; }
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getNationalId() { return nationalId; }
    public String getAddress() { return address; }
    public String getOrganization() { return organization; }
    public String getLicenseNumber() { return licenseNumber; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getHeightCm() { return heightCm; }
    public Integer getExperienceYear() { return experienceYear; }
    public Integer getTotalRaces() { return totalRaces; }
    public Integer getTotalWins() { return totalWins; }
    public String getBadgeNumber() { return badgeNumber; }
    public String getSpeciality() { return speciality; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
