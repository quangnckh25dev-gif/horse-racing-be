package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BetResponse {
    private Integer betId;
    private Integer userId;
    private Integer raceId;
    private String raceName;
    private Integer entryId;
    private String horseName;
    private String betType;
    private String betTypeLabel;
    private Integer targetPosition;
    private BigDecimal amount;
    private BigDecimal odds;
    private BigDecimal potentialPayout;
    private BigDecimal actualPayout;
    private String status;
    private String statusLabel;
    private Boolean settled;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    public BetResponse(Integer betId, Integer userId, Integer raceId, Integer entryId,
                       String betType, BigDecimal amount, BigDecimal odds, BigDecimal potentialPayout,
                       String status, LocalDateTime createdAt, LocalDateTime settledAt) {
        this(betId, userId, raceId, null, entryId, null, betType, null, amount, odds, potentialPayout,
                status, createdAt, settledAt);
    }

    public BetResponse(Integer betId, Integer userId, Integer raceId, String raceName, Integer entryId,
                       String horseName, String betType, BigDecimal amount, BigDecimal odds,
                       BigDecimal potentialPayout, String status, LocalDateTime createdAt, LocalDateTime settledAt) {
        this(betId, userId, raceId, raceName, entryId, horseName, betType, null, amount, odds,
                potentialPayout, status, createdAt, settledAt);
    }

    public BetResponse(Integer betId, Integer userId, Integer raceId, String raceName, Integer entryId,
                       String horseName, String betType, Integer targetPosition, BigDecimal amount, BigDecimal odds,
                       BigDecimal potentialPayout, String status, LocalDateTime createdAt, LocalDateTime settledAt) {
        this.betId = betId;
        this.userId = userId;
        this.raceId = raceId;
        this.raceName = raceName;
        this.entryId = entryId;
        this.horseName = horseName;
        this.betType = betType;
        this.betTypeLabel = toBetTypeLabel(betType);
        this.targetPosition = targetPosition;
        this.amount = amount;
        this.odds = odds;
        this.potentialPayout = potentialPayout;
        this.actualPayout = calculateActualPayout(status, potentialPayout);
        this.status = status;
        this.statusLabel = toStatusLabel(status);
        this.settled = settledAt != null || "Won".equals(status) || "Lost".equals(status);
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    public Integer getBetId() { return betId; }
    public Integer getUserId() { return userId; }
    public Integer getRaceId() { return raceId; }
    public String getRaceName() { return raceName; }
    public Integer getEntryId() { return entryId; }
    public String getHorseName() { return horseName; }
    public String getBetType() { return betType; }
    public String getBetTypeLabel() { return betTypeLabel; }
    public Integer getTargetPosition() { return targetPosition; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOdds() { return odds; }
    public BigDecimal getPotentialPayout() { return potentialPayout; }
    public BigDecimal getActualPayout() { return actualPayout; }
    public BigDecimal getPayout() { return actualPayout; }
    public String getStatus() { return status; }
    public String getStatusLabel() { return statusLabel; }
    public Boolean getSettled() { return settled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }

    private String toBetTypeLabel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "WIN" -> "Ve nhat";
            case "PLACE" -> "Top 2";
            case "SHOW" -> "Top 3";
            case "EXACT" -> "Dung vi tri";
            default -> value;
        };
    }

    private String toStatusLabel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "Pending" -> "Cho ket qua";
            case "Won" -> "Thang cuoc";
            case "Lost" -> "Thua cuoc";
            case "Cancelled" -> "Da huy";
            default -> value;
        };
    }

    private BigDecimal calculateActualPayout(String status, BigDecimal potentialPayout) {
        if ("Won".equals(status)) {
            return potentialPayout;
        }
        if ("Lost".equals(status) || "Cancelled".equals(status)) {
            return BigDecimal.ZERO;
        }
        return null;
    }
}
