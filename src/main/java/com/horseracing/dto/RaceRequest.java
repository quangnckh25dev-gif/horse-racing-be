package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RaceRequest {

    private Integer tournamentId;
    private Integer roundId;
    private String raceName;
    private LocalDateTime raceDate;
    private Integer trackLength;
    private String trackType;
    private Integer maxParticipants;
    private BigDecimal prizeFirst;
    private BigDecimal prizeSecond;
    private BigDecimal prizeThird;
    private String status;
    private LocalDateTime registrationOpen;
    private LocalDateTime registrationClose;

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
}
