package com.horseracing.dto;

import java.util.List;

public class SharedDashboardResponse {
    private Integer totalRaces;
    private Integer upcomingRaces;
    private Integer ongoingRaces;
    private Integer finishedRaces;
    private Integer registrationOpenRaces;
    private Integer totalHorses;
    private Integer totalJockeys;
    private List<LeaderboardResponse> topJockeys;
    private List<LeaderboardResponse> topHorses;
    private List<RaceSummaryResponse> featuredRaces;

    public SharedDashboardResponse(Integer totalRaces, Integer upcomingRaces, Integer ongoingRaces,
                                   Integer finishedRaces, Integer registrationOpenRaces,
                                   Integer totalHorses, Integer totalJockeys,
                                   List<LeaderboardResponse> topJockeys,
                                   List<LeaderboardResponse> topHorses,
                                   List<RaceSummaryResponse> featuredRaces) {
        this.totalRaces = totalRaces;
        this.upcomingRaces = upcomingRaces;
        this.ongoingRaces = ongoingRaces;
        this.finishedRaces = finishedRaces;
        this.registrationOpenRaces = registrationOpenRaces;
        this.totalHorses = totalHorses;
        this.totalJockeys = totalJockeys;
        this.topJockeys = topJockeys;
        this.topHorses = topHorses;
        this.featuredRaces = featuredRaces;
    }

    public Integer getTotalRaces() {
        return totalRaces;
    }

    public Integer getUpcomingRaces() {
        return upcomingRaces;
    }

    public Integer getOngoingRaces() {
        return ongoingRaces;
    }

    public Integer getFinishedRaces() {
        return finishedRaces;
    }

    public Integer getRegistrationOpenRaces() {
        return registrationOpenRaces;
    }

    public Integer getTotalHorses() {
        return totalHorses;
    }

    public Integer getTotalJockeys() {
        return totalJockeys;
    }

    public List<LeaderboardResponse> getTopJockeys() {
        return topJockeys;
    }

    public List<LeaderboardResponse> getTopHorses() {
        return topHorses;
    }

    public List<RaceSummaryResponse> getFeaturedRaces() {
        return featuredRaces;
    }
}
