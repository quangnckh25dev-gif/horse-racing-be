package com.horseracing.dto;

import java.math.BigDecimal;

public class BetOptionResponse {
    private Integer entryId;
    private Integer horseId;
    private String horseName;
    private Integer jockeyId;
    private String jockeyName;
    private Integer laneNumber;
    private BigDecimal odds;

    public BetOptionResponse(Integer entryId, Integer horseId, String horseName, Integer jockeyId, BigDecimal odds) {
        this(entryId, horseId, horseName, jockeyId, null, null, odds);
    }

    public BetOptionResponse(Integer entryId, Integer horseId, String horseName, Integer jockeyId,
                             String jockeyName, Integer laneNumber, BigDecimal odds) {
        this.entryId = entryId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.jockeyId = jockeyId;
        this.jockeyName = jockeyName;
        this.laneNumber = laneNumber;
        this.odds = odds;
    }

    public Integer getEntryId() { return entryId; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Integer getJockeyId() { return jockeyId; }
    public String getJockeyName() { return jockeyName; }
    public Integer getLaneNumber() { return laneNumber; }
    public BigDecimal getOdds() { return odds; }
}
