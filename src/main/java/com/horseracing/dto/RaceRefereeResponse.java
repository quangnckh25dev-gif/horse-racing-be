package com.horseracing.dto;

import java.time.LocalDateTime;

public class RaceRefereeResponse {

    private Integer raceRefereeId;
    private Integer raceId;
    private Integer refereeId;
    private String role;
    private LocalDateTime assignedAt;

    public RaceRefereeResponse(Integer raceRefereeId, Integer raceId, Integer refereeId,
                               String role, LocalDateTime assignedAt) {
        this.raceRefereeId = raceRefereeId;
        this.raceId = raceId;
        this.refereeId = refereeId;
        this.role = role;
        this.assignedAt = assignedAt;
    }

    public Integer getRaceRefereeId() {
        return raceRefereeId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public Integer getRefereeId() {
        return refereeId;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
}
