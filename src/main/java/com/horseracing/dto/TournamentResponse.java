package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TournamentResponse {

    private Integer tournamentId;
    private String tournamentName;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal prizeFund;
    private String status;
    private Integer createdByAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TournamentResponse(Integer tournamentId, String tournamentName, String description, String location,
                              LocalDate startDate, LocalDate endDate, BigDecimal prizeFund, String status,
                              Integer createdByAdmin, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.prizeFund = prizeFund;
        this.status = status;
        this.createdByAdmin = createdByAdmin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getTournamentId() {
        return tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getPrizeFund() {
        return prizeFund;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCreatedByAdmin() {
        return createdByAdmin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
