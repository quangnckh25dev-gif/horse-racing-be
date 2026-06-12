package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Jockeys")
public class Jockey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JockeyID")
    private Integer jockeyId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "NationalID", nullable = false)
    private String nationalId;

    @Column(name = "LicenseNumber", nullable = false)
    private String licenseNumber;

    @Column(name = "WeightKg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "HeightCm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "ExperienceYear")
    private Integer experienceYear;

    @Column(name = "TotalRaces")
    private Integer totalRaces;

    @Column(name = "TotalWins")
    private Integer totalWins;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    public Integer getJockeyId() { return jockeyId; }
    public void setJockeyId(Integer jockeyId) { this.jockeyId = jockeyId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public Integer getExperienceYear() { return experienceYear; }
    public void setExperienceYear(Integer experienceYear) { this.experienceYear = experienceYear; }
    public Integer getTotalRaces() { return totalRaces; }
    public void setTotalRaces(Integer totalRaces) { this.totalRaces = totalRaces; }
    public Integer getTotalWins() { return totalWins; }
    public void setTotalWins(Integer totalWins) { this.totalWins = totalWins; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
