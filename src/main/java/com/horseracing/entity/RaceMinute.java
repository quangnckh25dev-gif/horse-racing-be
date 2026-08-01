package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "RaceMinutes")
public class RaceMinute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MinuteID")
    private Integer minuteId;

    @Column(name = "RaceID", nullable = false, unique = true)
    private Integer raceId;

    @Column(name = "RefereeID", nullable = false)
    private Integer refereeId;

    @Column(name = "Content")
    private String content;

    @Column(name = "WeatherCondition")
    private String weatherCondition;

    @Column(name = "PreRaceChecks")
    private String preRaceChecks;

    @Column(name = "PostRaceNotes")
    private String postRaceNotes;

    @Column(name = "MinutesFileURL")
    private String minutesFileUrl;

    @Column(name = "SentToOwners", nullable = false)
    private Boolean sentToOwners;

    @Column(name = "SentAt")
    private LocalDateTime sentAt;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    public Integer getMinuteId() {
        return minuteId;
    }

    public void setMinuteId(Integer minuteId) {
        this.minuteId = minuteId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public void setRaceId(Integer raceId) {
        this.raceId = raceId;
    }

    public Integer getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(Integer refereeId) {
        this.refereeId = refereeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getPreRaceChecks() {
        return preRaceChecks;
    }

    public void setPreRaceChecks(String preRaceChecks) {
        this.preRaceChecks = preRaceChecks;
    }

    public String getPostRaceNotes() {
        return postRaceNotes;
    }

    public void setPostRaceNotes(String postRaceNotes) {
        this.postRaceNotes = postRaceNotes;
    }

    public String getMinutesFileUrl() {
        return minutesFileUrl;
    }

    public void setMinutesFileUrl(String minutesFileUrl) {
        this.minutesFileUrl = minutesFileUrl;
    }

    public Boolean getSentToOwners() {
        return sentToOwners;
    }

    public void setSentToOwners(Boolean sentToOwners) {
        this.sentToOwners = sentToOwners;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
