package com.horseracing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TournamentRequest {

    @NotBlank(message = "tournamentName is required.")
    @Size(max = 200, message = "tournamentName must not exceed 200 characters.")
    private String tournamentName;

    @Size(max = 1000, message = "description must not exceed 1000 characters.")
    private String description;

    @Size(max = 300, message = "location must not exceed 300 characters.")
    private String location;

    @NotNull(message = "startDate is required.")
    private LocalDate startDate;

    @NotNull(message = "endDate is required.")
    private LocalDate endDate;

    @DecimalMin(value = "0.0", message = "budgetTotal cannot be negative.")
    private BigDecimal budgetTotal;

    @Positive(message = "maxHorses must be greater than 0.")
    private Integer maxHorses;

    @Positive(message = "maxParticipants must be greater than 0.")
    private Integer maxParticipants;

    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
    public Integer getMaxHorses() { return maxHorses; }
    public void setMaxHorses(Integer maxHorses) { this.maxHorses = maxHorses; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
}
