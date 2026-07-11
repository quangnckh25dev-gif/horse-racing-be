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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TournamentID")
    private Integer tournamentId;

    @Column(name = "TournamentName", nullable = false, length = 200)
    private String tournamentName;

    @Column(name = "Description", length = 1000)
    private String description;

    @Column(name = "Location", length = 300)
    private String location;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "EndDate", nullable = false)
    private LocalDate endDate;

    @Column(name = "BudgetTotal", precision = 18, scale = 2)
    private BigDecimal budgetTotal;

    @Column(name = "MaxHorses")
    private Integer maxHorses;

    @Column(name = "MaxParticipants")
    private Integer maxParticipants;

    @Column(name = "Status", nullable = false, length = 30)
    private String status;

    @Column(name = "CreatedBy")
    private Integer createdBy;

    @Column(name = "ApprovedByAdmin")
    private Integer approvedByAdmin;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @Column(name = "RejectReason", length = 500)
    private String rejectReason;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        status = status == null || status.isBlank() ? "Draft" : status;
        budgetTotal = budgetTotal == null ? BigDecimal.ZERO : budgetTotal;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getTournamentId() { return tournamentId; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public Integer getApprovedByAdmin() { return approvedByAdmin; }
    public void setApprovedByAdmin(Integer approvedByAdmin) { this.approvedByAdmin = approvedByAdmin; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
