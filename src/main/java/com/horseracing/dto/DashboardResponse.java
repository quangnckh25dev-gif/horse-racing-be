package com.horseracing.dto;

public class DashboardResponse {
    private Integer totalActiveUsers;
    private Integer pendingApprovals;
    private Integer ongoingTournaments;
    private Integer upcomingRaces;
    private Integer finishedRaces;
    private Integer totalHorses;
    private Integer totalJockeys;
    private Integer totalBets;
    private Integer wonBets;

    public DashboardResponse(Integer totalActiveUsers, Integer pendingApprovals, Integer ongoingTournaments,
                             Integer upcomingRaces, Integer finishedRaces, Integer totalHorses,
                             Integer totalJockeys, Integer totalBets, Integer wonBets) {
        this.totalActiveUsers = totalActiveUsers;
        this.pendingApprovals = pendingApprovals;
        this.ongoingTournaments = ongoingTournaments;
        this.upcomingRaces = upcomingRaces;
        this.finishedRaces = finishedRaces;
        this.totalHorses = totalHorses;
        this.totalJockeys = totalJockeys;
        this.totalBets = totalBets;
        this.wonBets = wonBets;
    }

    public Integer getTotalActiveUsers() { return totalActiveUsers; }
    public Integer getPendingApprovals() { return pendingApprovals; }
    public Integer getOngoingTournaments() { return ongoingTournaments; }
    public Integer getUpcomingRaces() { return upcomingRaces; }
    public Integer getFinishedRaces() { return finishedRaces; }
    public Integer getTotalHorses() { return totalHorses; }
    public Integer getTotalJockeys() { return totalJockeys; }
    public Integer getTotalBets() { return totalBets; }
    public Integer getWonBets() { return wonBets; }
}
