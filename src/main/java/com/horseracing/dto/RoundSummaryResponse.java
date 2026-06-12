package com.horseracing.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RoundSummaryResponse {

    private Integer roundId;
    private Integer tournamentId;
    private String roundName;
    private Integer roundOrder;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private LocalDateTime createdAt;

    public RoundSummaryResponse(Integer roundId, Integer tournamentId, String roundName, Integer roundOrder,
                                LocalDate startDate, LocalDate endDate, String description,
                                LocalDateTime createdAt) {
        this.roundId = roundId;
        this.tournamentId = tournamentId;
        this.roundName = roundName;
        this.roundOrder = roundOrder;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Integer getRoundId() {
        return roundId;
    }

    public Integer getTournamentId() {
        return tournamentId;
    }

    public String getRoundName() {
        return roundName;
    }

    public Integer getRoundOrder() {
        return roundOrder;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
