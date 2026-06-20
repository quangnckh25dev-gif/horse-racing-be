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
@Table(name = "Bets")
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BetID")
    private Integer betId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "EntryID", nullable = false)
    private Integer entryId;

    @Column(name = "BetType", nullable = false)
    private String betType;

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
        createdAt = LocalDateTime.now();
        if (betType == null || betType.isBlank()) {
            betType = "WIN";
        }
        if (odds == null) {
            odds = BigDecimal.valueOf(2);
        }
        if (potentialPayout == null && amount != null) {
            potentialPayout = amount.multiply(odds);
        }
        if (status == null || status.isBlank()) {
            status = "Pending";
        }
    }

    public Integer getBetId() { return betId; }
    public void setBetId(Integer betId) { this.betId = betId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public String getBetType() { return betType; }
    public void setBetType(String betType) { this.betType = betType; }
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
