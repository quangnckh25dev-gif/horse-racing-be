package com.horseracing.dto;

import java.util.List;

public record TournamentDetailResponse(
        TournamentResponse tournament,
        List<RoundSummaryResponse> rounds,
        List<RaceSummaryResponse> races
) {
}
