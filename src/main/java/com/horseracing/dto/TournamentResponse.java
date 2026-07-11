package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TournamentResponse(
        Integer tournamentId,
        String tournamentName,
        String description,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budgetTotal,
        Integer maxHorses,
        Integer maxParticipants,
        String status,
        Integer createdBy,
        Integer approvedByAdmin,
        LocalDateTime approvedAt,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
