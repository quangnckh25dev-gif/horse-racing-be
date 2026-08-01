package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BetResponse {
    private Integer betId;
    private Integer userId;
    private Integer raceId;
    private String raceName;
    private Integer entryId;
    private Integer horseId;
    private String horseName;
    private Integer jockeyId;
    private String jockeyName;
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
        this(betId, userId, raceId, raceName, entryId, null, horseName, null, null, betType, null, amount, odds,
                potentialPayout, status, createdAt, settledAt);
    }

    public BetResponse(Integer betId, Integer userId, Integer raceId, String raceName, Integer entryId,
                       String horseName, String betType, Integer targetPosition, BigDecimal amount, BigDecimal odds,
                       BigDecimal potentialPayout, String status, LocalDateTime createdAt, LocalDateTime settledAt) {
        this(betId, userId, raceId, raceName, entryId, null, horseName, null, null, betType, targetPosition,
                amount, odds, potentialPayout, status, createdAt, settledAt);
    }

    public BetResponse(Integer betId, Integer userId, Integer raceId, String raceName, Integer entryId,
                       Integer horseId, String horseName, Integer jockeyId, String jockeyName,
                       String betType, Integer targetPosition, BigDecimal amount, BigDecimal odds,
                       BigDecimal potentialPayout, String status, LocalDateTime createdAt, LocalDateTime settledAt) {
        this.betId = betId;
        this.userId = userId;
        this.raceId = raceId;
        this.raceName = raceName;
        this.entryId = entryId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.jockeyId = jockeyId;
        this.jockeyName = jockeyName;
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
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Integer getJockeyId() { return jockeyId; }
    public String getJockeyName() { return jockeyName; }
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
            case "WIN" -> "Win";
            case "PLACE" -> "Top 2";
            case "SHOW" -> "Top 3";
            case "EXACT" -> "Exact Position";
            case "EXACT_POSITION" -> "Exact Position";
            case "VIOLATION" -> "Violation";
            default -> value;
        };
    }

    private String toStatusLabel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "Pending" -> "Pending";
            case "Won" -> "Won";
            case "Lost" -> "Lost";
            case "Cancelled" -> "Cancelled";
            case "Refunded" -> "Refunded";
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
