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
    private String statusLabel;
    private Boolean bettingOpen;
    private String bettingStatusLabel;
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
        this.statusLabel = toStatusLabel(status);
        this.bettingOpen = isBettingOpen(status, raceDate);
        this.bettingStatusLabel = this.bettingOpen ? "Betting Open" : "Betting Closed";
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

    public LocalDateTime getStartTime() {
        return raceDate;
    }

    public Integer getTrackLength() {
        return trackLength;
    }

    public Integer getDistance() {
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

    public BigDecimal getPrizePool() {
        return safeMoney(prizeFirst).add(safeMoney(prizeSecond)).add(safeMoney(prizeThird));
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public Boolean getBettingOpen() {
        return bettingOpen;
    }

    public String getBettingStatusLabel() {
        return bettingStatusLabel;
    }

    public LocalDateTime getRegistrationOpen() {
        return registrationOpen;
    }

    public LocalDateTime getRegistrationClose() {
        return registrationClose;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String toStatusLabel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "Draft" -> "Draft";
            case "RegistrationOpen" -> "Registration Open";
            case "Ongoing" -> "Ongoing";
            case "Finished" -> "Finished";
            case "Cancelled" -> "Cancelled";
            default -> value;
        };
    }

    private boolean isBettingOpen(String status, LocalDateTime raceDate) {
        if (!"RegistrationOpen".equals(status)) {
            return false;
        }
        return raceDate == null || raceDate.isAfter(LocalDateTime.now());
    }
}
