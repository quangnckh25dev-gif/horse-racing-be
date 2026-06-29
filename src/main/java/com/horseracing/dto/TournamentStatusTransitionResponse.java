package com.horseracing.dto;

import java.util.List;

public class TournamentStatusTransitionResponse {
    private String currentStatus;
    private String currentLabel;
    private List<OptionResponse> nextStatuses;

    public TournamentStatusTransitionResponse(String currentStatus, String currentLabel, List<OptionResponse> nextStatuses) {
        this.currentStatus = currentStatus;
        this.currentLabel = currentLabel;
        this.nextStatuses = nextStatuses;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getCurrentLabel() {
        return currentLabel;
    }

    public List<OptionResponse> getNextStatuses() {
        return nextStatuses;
    }
}
