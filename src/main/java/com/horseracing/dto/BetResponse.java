package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BetResponse {
    private Integer betId;
    private Integer userId;
    private Integer raceId;
    private Integer entryId;
    private String betType;
    private BigDecimal amount;
    private BigDecimal odds;
    private BigDecimal potentialPayout;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    public BetResponse(Integer betId, Integer userId, Integer raceId, Integer entryId,
                       String betType, BigDecimal amount, BigDecimal odds, BigDecimal potentialPayout,
                       String status, LocalDateTime createdAt, LocalDateTime settledAt) {
        this.betId = betId;
        this.userId = userId;
        this.raceId = raceId;
        this.entryId = entryId;
        this.betType = betType;
        this.amount = amount;
        this.odds = odds;
        this.potentialPayout = potentialPayout;
        this.status = status;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    public Integer getBetId() { return betId; }
    public Integer getUserId() { return userId; }
    public Integer getRaceId() { return raceId; }
    public Integer getEntryId() { return entryId; }
    public String getBetType() { return betType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOdds() { return odds; }
    public BigDecimal getPotentialPayout() { return potentialPayout; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
}
