package com.horseracing.service;

import com.horseracing.dto.RoundRequest;
import com.horseracing.dto.RoundSummaryResponse;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RoundService {
    private static final Map<Integer, String> DEFAULT_ROUNDS = Map.of(
            1, "Qualify",
            2, "Semi Final",
            3, "Final"
    );

    private final RoundRepository roundRepository;
    private final TournamentRepository tournamentRepository;
    private final RaceRepository raceRepository;

    public RoundService(RoundRepository roundRepository, TournamentRepository tournamentRepository,
                        RaceRepository raceRepository) {
        this.roundRepository = roundRepository;
        this.tournamentRepository = tournamentRepository;
        this.raceRepository = raceRepository;
    }

    public List<RoundSummaryResponse> getRoundsByTournament(Integer tournamentId) {
        ensureTournamentExists(tournamentId);
        return roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoundSummaryResponse createRound(Integer tournamentId, RoundRequest request, User organizer) {
        getOwnedDraftTournament(tournamentId, organizer);
        throw new IllegalArgumentException("Default rounds are created automatically and cannot be added manually.");
    }

    @Transactional
    public RoundSummaryResponse updateRound(Integer roundId, RoundRequest request, User organizer) {
        Round round = getRoundOrThrow(roundId);
        Tournament tournament = getOwnedDraftTournament(round.getTournamentId(), organizer);
        validateDefaultRound(round);
        validateRoundDates(request, tournament);
        applyEditableFields(round, request);
        return toResponse(roundRepository.save(round));
    }

    @Transactional
    public void deleteRound(Integer roundId, User organizer) {
        Round round = getRoundOrThrow(roundId);
        getOwnedDraftTournament(round.getTournamentId(), organizer);
        throw new IllegalArgumentException("Default rounds cannot be deleted.");
    }

    private Tournament getOwnedDraftTournament(Integer tournamentId, User organizer) {
        requireOrganizer(organizer);
        Tournament tournament = tournamentRepository.findByTournamentIdAndCreatedBy(tournamentId, organizer.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Tournament does not belong to the current organizer."));
        if (!"Draft".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("Rounds can only be changed when the tournament is in Draft status.");
        }
        return tournament;
    }

    private Tournament ensureTournamentExists(Integer id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament was not found."));
    }

    private Round getRoundOrThrow(Integer id) {
        return roundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Round was not found."));
    }

    private void validateDefaultRound(Round round) {
        String expectedName = DEFAULT_ROUNDS.get(round.getRoundOrder());
        if (expectedName == null || !expectedName.equals(round.getRoundName())) {
            throw new IllegalArgumentException("Tournament rounds must be Qualify, Semi Final, and Final.");
        }
    }

    private void validateRoundDates(RoundRequest request, Tournament tournament) {
        if (request == null) {
            throw new IllegalArgumentException("Round data is required.");
        }
        if (request.getStartDate() != null && request.getStartDate().isBefore(tournament.getStartDate())) {
            throw new IllegalArgumentException("Round startDate is outside the tournament date range.");
        }
        if (request.getEndDate() != null && request.getEndDate().isAfter(tournament.getEndDate())) {
            throw new IllegalArgumentException("Round endDate is outside the tournament date range.");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate must be after startDate.");
        }
    }

    private void requireOrganizer(User user) {
        if (user == null || user.getRole() == null || !"Organizer".equals(user.getRole().getRoleName())) {
            throw new IllegalArgumentException("Only organizers can perform this action.");
        }
    }

    private void applyEditableFields(Round round, RoundRequest request) {
        round.setStartDate(request.getStartDate());
        round.setEndDate(request.getEndDate());
        round.setDescription(request.getDescription());
    }

    private RoundSummaryResponse toResponse(Round round) {
        return new RoundSummaryResponse(round.getRoundId(), round.getTournamentId(), round.getRoundName(),
                round.getRoundOrder(), round.getStartDate(), round.getEndDate(), round.getDescription(),
                round.getCreatedAt());
    }
}
