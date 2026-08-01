package com.horseracing.dto;

import java.util.List;

public class BettingHistoryResponse {
    private List<BetResponse> singleBets;
    private List<BetTicketResponse> parlayTickets;

    public BettingHistoryResponse(List<BetResponse> singleBets, List<BetTicketResponse> parlayTickets) {
        this.singleBets = singleBets;
        this.parlayTickets = parlayTickets;
    }

    public List<BetResponse> getSingleBets() { return singleBets; }
    public List<BetTicketResponse> getParlayTickets() { return parlayTickets; }
}
