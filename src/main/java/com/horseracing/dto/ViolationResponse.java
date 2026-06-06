package com.horseracing.dto;

import java.time.LocalDateTime;

public class ViolationResponse {

    private Integer violationId;
    private Integer raceId;
    private Integer entryId;
    private Integer refereeId;
    private String violationType;
    private String description;
    private String penalty;
    private LocalDateTime recordedAt;

    public ViolationResponse(Integer violationId, Integer raceId, Integer entryId, Integer refereeId,
                             String violationType, String description, String penalty, LocalDateTime recordedAt) {
        this.violationId = violationId;
        this.raceId = raceId;
        this.entryId = entryId;
        this.refereeId = refereeId;
        this.violationType = violationType;
        this.description = description;
        this.penalty = penalty;
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

    public String getDescription() {
        return description;
    }

    public String getPenalty() {
        return penalty;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
