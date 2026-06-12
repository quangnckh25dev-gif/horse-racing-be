package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "RaceEntries")
public class RaceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EntryID")
    private Integer entryId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "HorseID", nullable = false)
    private Integer horseId;

    @Column(name = "JockeyID")
    private Integer jockeyId;

    @Column(name = "LaneNumber")
    private Integer laneNumber;

    @Column(name = "RegistrationStatus", nullable = false)
    private String registrationStatus;

    @Column(name = "OwnerConfirmed", nullable = false)
    private Boolean ownerConfirmed;

    @Column(name = "JockeyConfirmed", nullable = false)
    private Boolean jockeyConfirmed;

    @Column(name = "AdminApproved", nullable = false)
    private Boolean adminApproved;

    @Column(name = "RegisteredAt")
    private LocalDateTime registeredAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        registeredAt = now;
        updatedAt = now;
        if (registrationStatus == null || registrationStatus.isBlank()) {
            registrationStatus = "Pending";
        }
        if (ownerConfirmed == null) {
            ownerConfirmed = true;
        }
        if (jockeyConfirmed == null) {
            jockeyConfirmed = false;
        }
        if (adminApproved == null) {
            adminApproved = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public Integer getHorseId() { return horseId; }
    public void setHorseId(Integer horseId) { this.horseId = horseId; }
    public Integer getJockeyId() { return jockeyId; }
    public void setJockeyId(Integer jockeyId) { this.jockeyId = jockeyId; }
    public Integer getLaneNumber() { return laneNumber; }
    public void setLaneNumber(Integer laneNumber) { this.laneNumber = laneNumber; }
    public String getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(String registrationStatus) { this.registrationStatus = registrationStatus; }
    public Boolean getOwnerConfirmed() { return ownerConfirmed; }
    public void setOwnerConfirmed(Boolean ownerConfirmed) { this.ownerConfirmed = ownerConfirmed; }
    public Boolean getJockeyConfirmed() { return jockeyConfirmed; }
    public void setJockeyConfirmed(Boolean jockeyConfirmed) { this.jockeyConfirmed = jockeyConfirmed; }
    public Boolean getAdminApproved() { return adminApproved; }
    public void setAdminApproved(Boolean adminApproved) { this.adminApproved = adminApproved; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}