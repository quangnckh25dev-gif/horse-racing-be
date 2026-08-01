package com.horseracing.dto;

import java.time.LocalDateTime;

public class RaceMinuteResponse {

    private Integer minuteId;
    private Integer raceId;
    private Integer refereeId;
    private String content;
    private String weatherCondition;
    private String preRaceChecks;
    private String postRaceNotes;
    private String minutesFileUrl;
    private Boolean sentToOwners;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RaceMinuteResponse(Integer minuteId, Integer raceId, Integer refereeId, String content,
                              String weatherCondition, String preRaceChecks, String postRaceNotes,
                              String minutesFileUrl, Boolean sentToOwners, LocalDateTime sentAt,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.minuteId = minuteId;
        this.raceId = raceId;
        this.refereeId = refereeId;
        this.content = content;
        this.weatherCondition = weatherCondition;
        this.preRaceChecks = preRaceChecks;
        this.postRaceNotes = postRaceNotes;
        this.minutesFileUrl = minutesFileUrl;
        this.sentToOwners = sentToOwners;
        this.sentAt = sentAt;
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

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public String getPreRaceChecks() {
        return preRaceChecks;
    }

    public String getPostRaceNotes() {
        return postRaceNotes;
    }

    public String getMinutesFileUrl() {
        return minutesFileUrl;
    }

    public Boolean getSentToOwners() {
        return sentToOwners;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
