package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ViolationResponse {

    private Integer violationId;
    private Integer raceId;
    private Integer entryId;
    private Integer refereeId;
    private String violationType;
    private BigDecimal penaltySeconds;
    private Boolean isDq;
    private String evidenceImageUrl;
    private String description;
    private LocalDateTime recordedAt;

    public ViolationResponse(Integer violationId, Integer raceId, Integer entryId, Integer refereeId,
                             String violationType, BigDecimal penaltySeconds, Boolean isDq,
                             String evidenceImageUrl, String description, LocalDateTime recordedAt) {
        this.violationId = violationId;
        this.raceId = raceId;
        this.entryId = entryId;
        this.refereeId = refereeId;
        this.violationType = violationType;
        this.penaltySeconds = penaltySeconds;
        this.isDq = isDq;
        this.evidenceImageUrl = evidenceImageUrl;
        this.description = description;
        this.recordedAt = recordedAt;
    }

    public Integer getViolationId() {
        return violationId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public Integer getEntryId() {
        return entryId;
    }

    public Integer getRefereeId() {
        return refereeId;
    }

    public String getViolationType() {
        return violationType;
    }

    public BigDecimal getPenaltySeconds() {
        return penaltySeconds;
    }

    public Boolean getIsDq() {
        return isDq;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
