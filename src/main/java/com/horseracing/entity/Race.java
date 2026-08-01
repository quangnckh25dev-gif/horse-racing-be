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
@Table(name = "Races")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RaceID")
    private Integer raceId;

    @Column(name = "TournamentID", nullable = false)
    private Integer tournamentId;

    @Column(name = "RoundID")
    private Integer roundId;

    @Column(name = "RaceName", nullable = false)
    private String raceName;

    @Column(name = "RaceDate", nullable = false)
    private LocalDateTime raceDate;

    @Column(name = "TrackLength")
    private Integer trackLength;

    @Column(name = "TrackType")
    private String trackType;

    @Column(name = "MaxParticipants")
    private Integer maxParticipants;

    @Column(name = "PrizeFirst", precision = 18, scale = 2)
    private BigDecimal prizeFirst;

    @Column(name = "PrizeSecond", precision = 18, scale = 2)
    private BigDecimal prizeSecond;

    @Column(name = "PrizeThird", precision = 18, scale = 2)
    private BigDecimal prizeThird;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "RegistrationOpen")
    private LocalDateTime registrationOpen;

    @Column(name = "RegistrationClose")
    private LocalDateTime registrationClose;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "Draft";
        }
        if (prizeFirst == null) {
            prizeFirst = BigDecimal.ZERO;
        }
        if (prizeSecond == null) {
            prizeSecond = BigDecimal.ZERO;
        }
        if (prizeThird == null) {
            prizeThird = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getRaceId() {
        return raceId;
    }

    public void setRaceId(Integer raceId) {
        this.raceId = raceId;
    }

    public Integer getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Integer tournamentId) {
        this.tournamentId = tournamentId;
    }

    public Integer getRoundId() {
        return roundId;
    }

    public void setRoundId(Integer roundId) {
        this.roundId = roundId;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public LocalDateTime getRaceDate() {
        return raceDate;
    }

    public void setRaceDate(LocalDateTime raceDate) {
        this.raceDate = raceDate;
    }

    public Integer getTrackLength() {
        return trackLength;
    }

    public void setTrackLength(Integer trackLength) {
        this.trackLength = trackLength;
    }

    public String getTrackType() {
        return trackType;
    }

    public void setTrackType(String trackType) {
        this.trackType = trackType;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public BigDecimal getPrizeFirst() {
        return prizeFirst;
    }

    public void setPrizeFirst(BigDecimal prizeFirst) {
        this.prizeFirst = prizeFirst;
    }

    public BigDecimal getPrizeSecond() {
        return prizeSecond;
    }

    public void setPrizeSecond(BigDecimal prizeSecond) {
        this.prizeSecond = prizeSecond;
    }

    public BigDecimal getPrizeThird() {
        return prizeThird;
    }

    public void setPrizeThird(BigDecimal prizeThird) {
        this.prizeThird = prizeThird;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegistrationOpen() {
        return registrationOpen;
    }

    public void setRegistrationOpen(LocalDateTime registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    public LocalDateTime getRegistrationClose() {
        return registrationClose;
    }

    public void setRegistrationClose(LocalDateTime registrationClose) {
        this.registrationClose = registrationClose;
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
