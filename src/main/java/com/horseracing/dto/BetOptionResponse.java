package com.horseracing.dto;

import java.math.BigDecimal;

public class BetOptionResponse {
    private Integer entryId;
    private Integer horseId;
    private String horseName;
    private Integer jockeyId;
    private String jockeyName;
    private Integer laneNumber;
    private Integer horseRank;
    private String betType;
    private String betTypeLabel;
    private Integer targetPosition;
    private BigDecimal baseOdds;
    private BigDecimal odds;

    public BetOptionResponse(Integer entryId, Integer horseId, String horseName, Integer jockeyId, BigDecimal odds) {
        this(entryId, horseId, horseName, jockeyId, null, null, null, "WIN", null, odds, odds);
    }

    public BetOptionResponse(Integer entryId, Integer horseId, String horseName, Integer jockeyId,
                             String jockeyName, Integer laneNumber, BigDecimal odds) {
        this(entryId, horseId, horseName, jockeyId, jockeyName, laneNumber, null, "WIN", null, odds, odds);
    }

    public BetOptionResponse(Integer entryId, Integer horseId, String horseName, Integer jockeyId,
                             String jockeyName, Integer laneNumber, Integer horseRank, String betType,
                             Integer targetPosition, BigDecimal baseOdds, BigDecimal odds) {
        this.entryId = entryId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.jockeyId = jockeyId;
        this.jockeyName = jockeyName;
        this.laneNumber = laneNumber;
        this.horseRank = horseRank;
        this.betType = betType;
        this.betTypeLabel = toBetTypeLabel(betType);
        this.targetPosition = targetPosition;
        this.baseOdds = baseOdds;
        this.odds = odds;
    }

    public Integer getEntryId() { return entryId; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Integer getJockeyId() { return jockeyId; }
    public String getJockeyName() { return jockeyName; }
    public Integer getLaneNumber() { return laneNumber; }
    public Integer getHorseRank() { return horseRank; }
    public String getBetType() { return betType; }
    public String getBetTypeLabel() { return betTypeLabel; }
    public Integer getTargetPosition() { return targetPosition; }
    public BigDecimal getBaseOdds() { return baseOdds; }
    public BigDecimal getOdds() { return odds; }

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
}
