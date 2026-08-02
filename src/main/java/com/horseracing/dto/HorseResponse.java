package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HorseResponse {
    private Integer horseId;
    private Integer ownerId;
    private String horseName;
    private String breed;
    private Integer birthYear;
    private Integer age;
    private String color;
    private String gender;
    private BigDecimal weightKg;
    private String registerCode;
    private String healthStatus;
    private String photoUrl;
    private String status;
    private Boolean active;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HorseResponse(Integer horseId, Integer ownerId, String horseName, String breed, Integer birthYear,
                         Integer age, String color, String gender, BigDecimal weightKg, String registerCode,
                         String healthStatus, String photoUrl, String status, Boolean active,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(horseId, ownerId, horseName, breed, birthYear, age, color, gender, weightKg, registerCode,
                healthStatus, photoUrl, status, active, false, createdAt, updatedAt);
    }

    public HorseResponse(Integer horseId, Integer ownerId, String horseName, String breed, Integer birthYear,
                         Integer age, String color, String gender, BigDecimal weightKg, String registerCode,
                         String healthStatus, String photoUrl, String status, Boolean active, Boolean deleted,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.horseId = horseId;
        this.ownerId = ownerId;
        this.horseName = horseName;
        this.breed = breed;
        this.birthYear = birthYear;
        this.age = age;
        this.color = color;
        this.gender = gender;
        this.weightKg = weightKg;
        this.registerCode = registerCode;
        this.healthStatus = healthStatus;
        this.photoUrl = photoUrl;
        this.status = status;
        this.active = active;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getHorseId() { return horseId; }
    public Integer getOwnerId() { return ownerId; }
    public String getHorseName() { return horseName; }
    public String getBreed() { return breed; }
    public Integer getBirthYear() { return birthYear; }
    public Integer getAge() { return age; }
    public String getColor() { return color; }
    public String getGender() { return gender; }
    public BigDecimal getWeightKg() { return weightKg; }
    public String getRegisterCode() { return registerCode; }
    public String getHealthStatus() { return healthStatus; }
    public String getPhotoUrl() { return photoUrl; }
    public String getStatus() { return status; }
    public Boolean getActive() { return active; }
    public Boolean getDeleted() { return deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
