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
@Table(name = "Violations")
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ViolationID")
    private Integer violationId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "EntryID", nullable = false)
    private Integer entryId;

    @Column(name = "RefereeID", nullable = false)
    private Integer refereeId;

    @Column(name = "ViolationType", nullable = false)
    private String violationType;

    @Column(name = "PenaltySeconds", nullable = false, precision = 5, scale = 2)
    private BigDecimal penaltySeconds;

    @Column(name = "IsDQ", nullable = false)
    private Boolean isDq;

    @Column(name = "EvidenceImageURL")
    private String evidenceImageUrl;

    @Column(name = "Description")
    private String description;

    @Column(name = "RecordedAt")
    private LocalDateTime recordedAt;

    public Integer getViolationId() {
        return violationId;
    }

    public void setViolationId(Integer violationId) {
        this.violationId = violationId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public void setRaceId(Integer raceId) {
        this.raceId = raceId;
    }

    public Integer getEntryId() {
        return entryId;
    }

    public void setEntryId(Integer entryId) {
        this.entryId = entryId;
    }

    public Integer getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(Integer refereeId) {
        this.refereeId = refereeId;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPenaltySeconds() {
        return penaltySeconds;
    }

    public void setPenaltySeconds(BigDecimal penaltySeconds) {
        this.penaltySeconds = penaltySeconds;
    }

    public Boolean getIsDq() {
        return isDq;
    }

    public void setIsDq(Boolean dq) {
        isDq = dq;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public void setEvidenceImageUrl(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
