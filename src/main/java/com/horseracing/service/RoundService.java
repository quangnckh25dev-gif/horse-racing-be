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

@Service
@Transactional(readOnly = true)
public class RoundService {
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
                .map(this::toResponse).toList();
    }

    @Transactional
    //của buiquangann
    public RoundSummaryResponse createRound(Integer tournamentId, RoundRequest request, User organizer) {
        Tournament tournament = getOwnedDraftTournament(tournamentId, organizer);
        validateRequest(request, tournament);
        if (roundRepository.existsByTournamentIdAndRoundOrder(tournamentId, request.getRoundOrder())) {
            throw new IllegalArgumentException("roundOrder already exists in this tournament.");
        }
        Round round = new Round();
        round.setTournamentId(tournamentId);
        applyRequest(round, request);
        return toResponse(roundRepository.save(round));
    }

    @Transactional
    public RoundSummaryResponse updateRound(Integer roundId, RoundRequest request, User organizer) {
        Round round = getRoundOrThrow(roundId);
        Tournament tournament = getOwnedDraftTournament(round.getTournamentId(), organizer);
        validateRequest(request, tournament);
        if (roundRepository.existsByTournamentIdAndRoundOrderAndRoundIdNot(
                round.getTournamentId(), request.getRoundOrder(), roundId)) {
            throw new IllegalArgumentException("roundOrder already exists in this tournament.");
        }
        applyRequest(round, request);
        return toResponse(roundRepository.save(round));
    }

    @Transactional
    public void deleteRound(Integer roundId, User organizer) {
        Round round = getRoundOrThrow(roundId);
        getOwnedDraftTournament(round.getTournamentId(), organizer);
        if (raceRepository.existsByRoundId(roundId)) {
            throw new IllegalArgumentException("Rounds that already have races cannot be deleted.");
        }
        roundRepository.delete(round);
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

    private void validateRequest(RoundRequest request, Tournament tournament) {
        if (request == null || request.getRoundName() == null || request.getRoundName().isBlank())
            throw new IllegalArgumentException("roundName is required.");
        if (request.getRoundOrder() == null || request.getRoundOrder() <= 0)
            throw new IllegalArgumentException("roundOrder must be greater than 0.");
        if (request.getStartDate() != null && request.getStartDate().isBefore(tournament.getStartDate()))
            throw new IllegalArgumentException("Round startDate is outside the tournament date range.");
        if (request.getEndDate() != null && request.getEndDate().isAfter(tournament.getEndDate()))
            throw new IllegalArgumentException("Round endDate is outside the tournament date range.");
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate()))
            throw new IllegalArgumentException("endDate must be after startDate.");
    }

    private void requireOrganizer(User user) {
        if (user == null || user.getRole() == null || !"Organizer".equals(user.getRole().getRoleName()))
            throw new IllegalArgumentException("Only organizers can perform this action.");
    }

    private void applyRequest(Round round, RoundRequest request) {
        round.setRoundName(request.getRoundName().trim());
        round.setRoundOrder(request.getRoundOrder());
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
