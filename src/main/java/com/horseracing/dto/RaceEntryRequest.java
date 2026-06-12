package com.horseracing.dto;

public class RaceEntryRequest {
    private Integer horseId;
    private Integer jockeyId;
    private Integer laneNumber;

    public Integer getHorseId() { return horseId; }
    public void setHorseId(Integer horseId) { this.horseId = horseId; }
    public Integer getJockeyId() { return jockeyId; }
    public void setJockeyId(Integer jockeyId) { this.jockeyId = jockeyId; }
    public Integer getLaneNumber() { return laneNumber; }
    public void setLaneNumber(Integer laneNumber) { this.laneNumber = laneNumber; }
}