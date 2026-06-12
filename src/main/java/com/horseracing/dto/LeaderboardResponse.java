package com.horseracing.dto;

import java.math.BigDecimal;

public class LeaderboardResponse {
    private Integer rank;
    private Integer entityId;
    private String name;
    private Integer totalRaces;
    private Integer totalWins;
    private Integer totalPodiums;
    private BigDecimal totalPrize;
    private Integer points;

    public LeaderboardResponse(Integer rank, Integer entityId, String name, Integer totalRaces,
                               Integer totalWins, Integer totalPodiums, BigDecimal totalPrize,
                               Integer points) {
        this.rank = rank;
        this.entityId = entityId;
        this.name = name;
        this.totalRaces = totalRaces;
        this.totalWins = totalWins;
        this.totalPodiums = totalPodiums;
        this.totalPrize = totalPrize;
        this.points = points;
    }

    public Integer getRank() {
        return rank;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public Integer getTotalRaces() {
        return totalRaces;
    }

    public Integer getTotalWins() {
        return totalWins;
    }

    public Integer getTotalPodiums() {
        return totalPodiums;
    }

    public BigDecimal getTotalPrize() {
        return totalPrize;
    }

    public Integer getPoints() {
        return points;
    }
}
