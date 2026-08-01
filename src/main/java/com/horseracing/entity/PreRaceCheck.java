package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "PreRaceChecks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"RaceID", "EntryID"})
        }
)
public class PreRaceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PreRaceCheckID")
    private Integer preRaceCheckId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "EntryID", nullable = false)
    private Integer entryId;

    @Column(name = "HorseID", nullable = false)
    private Integer horseId;

    @Column(name = "RefereeID", nullable = false)
    private Integer refereeId;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "CheckedAt")
    private LocalDateTime checkedAt;

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) {
            status = "Pending";
        }
    }

    public Integer getPreRaceCheckId() { return preRaceCheckId; }
    public void setPreRaceCheckId(Integer preRaceCheckId) { this.preRaceCheckId = preRaceCheckId; }
    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public Integer getHorseId() { return horseId; }
    public void setHorseId(Integer horseId) { this.horseId = horseId; }
    public Integer getRefereeId() { return refereeId; }
    public void setRefereeId(Integer refereeId) { this.refereeId = refereeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
