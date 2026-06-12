package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TournamentDetailResponse extends TournamentResponse {

    private List<RoundSummaryResponse> rounds;
    private List<RaceSummaryResponse> races;

    public TournamentDetailResponse(Integer tournamentId, String tournamentName, String description, String location,
                                    LocalDate startDate, LocalDate endDate, BigDecimal prizeFund, String status,
                                    Integer createdByAdmin, LocalDateTime createdAt, LocalDateTime updatedAt,
                                    List<RoundSummaryResponse> rounds, List<RaceSummaryResponse> races) {
        super(tournamentId, tournamentName, description, location, startDate, endDate, prizeFund, status,
                createdByAdmin, createdAt, updatedAt);
        this.rounds = rounds;
        this.races = races;
    }

    public List<RoundSummaryResponse> getRounds() {
        return rounds;
    }

    public List<RaceSummaryResponse> getRaces() {
        return races;
    }
}
