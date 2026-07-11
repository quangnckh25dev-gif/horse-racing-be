package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Horses")
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HorseID")
    private Integer horseId;

    @Column(name = "OwnerID", nullable = false)
    private Integer ownerId;

    @Column(name = "HorseName", nullable = false)
    private String horseName;

    @Column(name = "Breed")
    private String breed;

    @Column(name = "BirthYear")
    private Integer birthYear;

    @Column(name = "Color")
    private String color;

    @Column(name = "Gender")
    private String gender;

    @Column(name = "WeightKg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "RegisterCode")
    private String registerCode;

    @Column(name = "HealthStatus")
    private String healthStatus;

    @Column(name = "HealthUpdatedBy")
    private Integer healthUpdatedBy;

    @Column(name = "HealthUpdatedAt")
    private LocalDateTime healthUpdatedAt;

    @Column(name = "PhotoURL")
    private String photoUrl;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getHorseId() { return horseId; }
    public void setHorseId(Integer horseId) { this.horseId = horseId; }
    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    public String getHorseName() { return horseName; }
    public void setHorseName(String horseName) { this.horseName = horseName; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getRegisterCode() { return registerCode; }
    public void setRegisterCode(String registerCode) { this.registerCode = registerCode; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public Integer getHealthUpdatedBy() { return healthUpdatedBy; }
    public void setHealthUpdatedBy(Integer healthUpdatedBy) { this.healthUpdatedBy = healthUpdatedBy; }
    public LocalDateTime getHealthUpdatedAt() { return healthUpdatedAt; }
    public void setHealthUpdatedAt(LocalDateTime healthUpdatedAt) { this.healthUpdatedAt = healthUpdatedAt; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
