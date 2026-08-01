package com.horseracing.dto;

import java.math.BigDecimal;

public class BetSelectionResponse {
    private Integer selectionId;
    private Integer entryId;
    private Integer horseId;
    private String horseName;
    private String betType;
    private String betTypeLabel;
    private Integer targetPosition;
    private BigDecimal odds;
    private Boolean resolved;
    private Boolean won;

    public BetSelectionResponse(Integer selectionId, Integer entryId, Integer horseId, String horseName,
                                String betType, Integer targetPosition, BigDecimal odds,
                                Boolean resolved, Boolean won) {
        this.selectionId = selectionId;
        this.entryId = entryId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.betType = betType;
        this.betTypeLabel = toBetTypeLabel(betType);
        this.targetPosition = targetPosition;
        this.odds = odds;
        this.resolved = resolved;
        this.won = won;
    }

    public Integer getSelectionId() { return selectionId; }
    public Integer getEntryId() { return entryId; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public String getBetType() { return betType; }
    public String getBetTypeLabel() { return betTypeLabel; }
    public Integer getTargetPosition() { return targetPosition; }
    public BigDecimal getOdds() { return odds; }
    public Boolean getResolved() { return resolved; }
    public Boolean getWon() { return won; }

    private String toBetTypeLabel(String value) {
        if (value == null) return null;
        return switch (value) {
            case "WIN" -> "Win";
            case "EXACT_POSITION" -> "Exact Position";
            case "VIOLATION" -> "Violation";
            default -> value;
        };
    }
}
