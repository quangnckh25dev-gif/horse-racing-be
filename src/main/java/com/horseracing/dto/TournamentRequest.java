package com.horseracing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TournamentRequest {

    @NotBlank(message = "tournamentName khong duoc de trong")
    @Size(max = 200, message = "tournamentName khong duoc vuot qua 200 ky tu")
    private String tournamentName;

    @Size(max = 1000, message = "description khong duoc vuot qua 1000 ky tu")
    private String description;

    @Size(max = 300, message = "location khong duoc vuot qua 300 ky tu")
    private String location;

    @NotNull(message = "startDate khong duoc de trong")
    private LocalDate startDate;

    @NotNull(message = "endDate khong duoc de trong")
    private LocalDate endDate;

    @DecimalMin(value = "0.0", message = "budgetTotal khong duoc am")
    private BigDecimal budgetTotal;

    @Positive(message = "maxHorses phai lon hon 0")
    private Integer maxHorses;

    @Positive(message = "maxParticipants phai lon hon 0")
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
