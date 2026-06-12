package com.horseracing.service;

import com.horseracing.dto.RoundRequest;
import com.horseracing.dto.RoundSummaryResponse;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoundService {

    private final RoundRepository roundRepository;
    private final TournamentRepository tournamentRepository;
    private final RaceRepository raceRepository;

    public RoundService(RoundRepository roundRepository,
                        TournamentRepository tournamentRepository,
                        RaceRepository raceRepository) {
        this.roundRepository = roundRepository;
        this.tournamentRepository = tournamentRepository;
        this.raceRepository = raceRepository;
    }

    public List<RoundSummaryResponse> getRoundsByTournament(Integer tournamentId) {
        ensureTournamentExists(tournamentId);
        return roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoundSummaryResponse createRound(Integer tournamentId, RoundRequest request) {
        Tournament tournament = ensureTournamentExists(tournamentId);
        validateRoundRequest(request, tournament);

        Round round = new Round();
        round.setTournamentId(tournamentId);
        applyRequest(round, request);

        return toResponse(roundRepository.save(round));
    }

    public RoundSummaryResponse updateRound(Integer roundId, RoundRequest request) {
        Round round = getRoundOrThrow(roundId);
        Tournament tournament = ensureTournamentExists(round.getTournamentId());
        validateRoundRequest(request, tournament);
        applyRequest(round, request);

        return toResponse(roundRepository.save(round));
    }

    public void deleteRound(Integer roundId) {
        Round round = getRoundOrThrow(roundId);
        if (raceRepository.existsByRoundId(round.getRoundId())) {
            throw new IllegalArgumentException("Khong the xoa round da co race");
        }
        roundRepository.delete(round);
    }

    private Tournament ensureTournamentExists(Integer tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("Tournament id khong hop le");
        }

        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament"));
    }

    private Round getRoundOrThrow(Integer roundId) {
        if (roundId == null) {
            throw new IllegalArgumentException("Round id khong hop le");
        }

        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay round"));
    }

    private void validateRoundRequest(RoundRequest request, Tournament tournament) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu round khong duoc de trong");
        }
        if (request.getRoundName() == null || request.getRoundName().isBlank()) {
            throw new IllegalArgumentException("RoundName khong duoc de trong");
        }
        if (request.getRoundOrder() == null || request.getRoundOrder() <= 0) {
            throw new IllegalArgumentException("RoundOrder phai lon hon 0");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("EndDate phai lon hon hoac bang StartDate");
        }
        if (request.getStartDate() != null && request.getStartDate().isBefore(tournament.getStartDate())) {
            throw new IllegalArgumentException("StartDate cua round khong duoc truoc StartDate cua tournament");
        }
        if (request.getEndDate() != null && request.getEndDate().isAfter(tournament.getEndDate())) {
            throw new IllegalArgumentException("EndDate cua round khong duoc sau EndDate cua tournament");
        }
    }

    private void applyRequest(Round round, RoundRequest request) {
        round.setRoundName(request.getRoundName());
        round.setRoundOrder(request.getRoundOrder());
        round.setStartDate(request.getStartDate());
        round.setEndDate(request.getEndDate());
        round.setDescription(request.getDescription());
    }

    private RoundSummaryResponse toResponse(Round round) {
        return new RoundSummaryResponse(
                round.getRoundId(),
                round.getTournamentId(),
                round.getRoundName(),
                round.getRoundOrder(),
                round.getStartDate(),
                round.getEndDate(),
                round.getDescription(),
                round.getCreatedAt()
        );
    }
}
