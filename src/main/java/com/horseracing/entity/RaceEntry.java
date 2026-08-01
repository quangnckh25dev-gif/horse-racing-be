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

    @Column(name = "OrganizerApproved", nullable = false)
    private Boolean organizerApproved;

    @Column(name = "ApprovedBy")
    private Integer approvedBy;

    @Column(name = "RejectReason")
    private String rejectReason;

    @Column(name = "RoundStatus")
    private String roundStatus;

    @Column(name = "EliminationRoundID")
    private Integer eliminationRoundId;

    @Column(name = "EliminationReason")
    private String eliminationReason;

    @Column(name = "JockeyConfirmed", nullable = false)
    private Boolean jockeyConfirmed;

    @Column(name = "Odds", nullable = false, precision = 10, scale = 2)
    private BigDecimal odds;

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
        if (organizerApproved == null) {
            organizerApproved = false;
        }
        if (jockeyConfirmed == null) {
            jockeyConfirmed = false;
        }
        if (odds == null) {
            odds = BigDecimal.valueOf(2);
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
    public Boolean getOrganizerApproved() { return organizerApproved; }
    public void setOrganizerApproved(Boolean organizerApproved) { this.organizerApproved = organizerApproved; }
    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getRoundStatus() { return roundStatus; }
    public void setRoundStatus(String roundStatus) { this.roundStatus = roundStatus; }
    public Integer getEliminationRoundId() { return eliminationRoundId; }
    public void setEliminationRoundId(Integer eliminationRoundId) { this.eliminationRoundId = eliminationRoundId; }
    public String getEliminationReason() { return eliminationReason; }
    public void setEliminationReason(String eliminationReason) { this.eliminationReason = eliminationReason; }
    public Boolean getJockeyConfirmed() { return jockeyConfirmed; }
    public void setJockeyConfirmed(Boolean jockeyConfirmed) { this.jockeyConfirmed = jockeyConfirmed; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
