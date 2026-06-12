package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RaceSummaryResponse {

    private Integer raceId;
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

    public RaceSummaryResponse(Integer raceId, Integer tournamentId, Integer roundId, String raceName,
                               LocalDateTime raceDate, Integer trackLength, String trackType,
                               Integer maxParticipants, BigDecimal prizeFirst, BigDecimal prizeSecond,
                               BigDecimal prizeThird, String status, LocalDateTime registrationOpen,
                               LocalDateTime registrationClose) {
        this.raceId = raceId;
        this.tournamentId = tournamentId;
        this.roundId = roundId;
        this.raceName = raceName;
        this.raceDate = raceDate;
        this.trackLength = trackLength;
        this.trackType = trackType;
        this.maxParticipants = maxParticipants;
        this.prizeFirst = prizeFirst;
        this.prizeSecond = prizeSecond;
        this.prizeThird = prizeThird;
        this.status = status;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public Integer getTournamentId() {
        return tournamentId;
    }

    public Integer getRoundId() {
        return roundId;
    }

    public String getRaceName() {
        return raceName;
    }

    public LocalDateTime getRaceDate() {
        return raceDate;
    }

    public Integer getTrackLength() {
        return trackLength;
    }

    public String getTrackType() {
        return trackType;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public BigDecimal getPrizeFirst() {
        return prizeFirst;
    }

    public BigDecimal getPrizeSecond() {
        return prizeSecond;
    }

    public BigDecimal getPrizeThird() {
        return prizeThird;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getRegistrationOpen() {
        return registrationOpen;
    }

    public LocalDateTime getRegistrationClose() {
        return registrationClose;
    }
}
