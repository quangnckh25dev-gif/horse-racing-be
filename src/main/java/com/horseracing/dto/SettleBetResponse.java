package com.horseracing.dto;

public class SettleBetResponse {
    private Integer raceId;
    private int totalBets;
    private int wonBets;
    private int lostBets;

    public SettleBetResponse(Integer raceId, int totalBets, int wonBets, int lostBets) {
        this.raceId = raceId;
        this.totalBets = totalBets;
        this.wonBets = wonBets;
        this.lostBets = lostBets;
    }

    public Integer getRaceId() { return raceId; }
    public int getTotalBets() { return totalBets; }
    public int getWonBets() { return wonBets; }
    public int getLostBets() { return lostBets; }
}
