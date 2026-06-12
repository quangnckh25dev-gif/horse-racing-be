package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TournamentRequest {

    private String tournamentName;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal prizeFund;
    private String status;
    private Integer createdByAdmin;

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getPrizeFund() {
        return prizeFund;
    }

    public void setPrizeFund(BigDecimal prizeFund) {
        this.prizeFund = prizeFund;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatedByAdmin() {
        return createdByAdmin;
    }

    public void setCreatedByAdmin(Integer createdByAdmin) {
        this.createdByAdmin = createdByAdmin;
    }
}
