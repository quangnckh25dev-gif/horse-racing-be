package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BetTickets")
public class BetTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketID")
    private Integer ticketId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "Odds", nullable = false, precision = 10, scale = 2)
    private BigDecimal odds;

    @Column(name = "PotentialPayout", nullable = false, precision = 18, scale = 2)
    private BigDecimal potentialPayout;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "SettledAt")
    private LocalDateTime settledAt;

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) {
            status = "Pending";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public BigDecimal getPotentialPayout() { return potentialPayout; }
    public void setPotentialPayout(BigDecimal potentialPayout) { this.potentialPayout = potentialPayout; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
}
