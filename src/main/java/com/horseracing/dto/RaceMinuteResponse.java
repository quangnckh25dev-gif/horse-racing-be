package com.horseracing.dto;

import java.time.LocalDateTime;

public class RaceMinuteResponse {

    private Integer minuteId;
    private Integer raceId;
    private Integer refereeId;
    private String content;
    private String preRaceChecks;
    private String postRaceNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RaceMinuteResponse(Integer minuteId, Integer raceId, Integer refereeId, String content,
                              String preRaceChecks, String postRaceNotes,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.minuteId = minuteId;
        this.raceId = raceId;
        this.refereeId = refereeId;
        this.content = content;
        this.preRaceChecks = preRaceChecks;
        this.postRaceNotes = postRaceNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getMinuteId() {
        return minuteId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public Integer getRefereeId() {
        return refereeId;
    }

    public String getContent() {
        return content;
    }

    public String getPreRaceChecks() {
        return preRaceChecks;
    }

    public String getPostRaceNotes() {
        return postRaceNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
