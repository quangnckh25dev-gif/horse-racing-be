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
            throw new IllegalArgumentException("roundOrder da ton tai trong tournament");
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
            throw new IllegalArgumentException("roundOrder da ton tai trong tournament");
        }
        applyRequest(round, request);
        return toResponse(roundRepository.save(round));
    }

    @Transactional
    public void deleteRound(Integer roundId, User organizer) {
        Round round = getRoundOrThrow(roundId);
        getOwnedDraftTournament(round.getTournamentId(), organizer);
        if (raceRepository.existsByRoundId(roundId)) {
            throw new IllegalArgumentException("Khong the xoa round da co race");
        }
        roundRepository.delete(round);
    }

    private Tournament getOwnedDraftTournament(Integer tournamentId, User organizer) {
        requireOrganizer(organizer);
        Tournament tournament = tournamentRepository.findByTournamentIdAndCreatedBy(tournamentId, organizer.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Tournament khong thuoc Organizer hien tai"));
        if (!"Draft".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("Chi duoc thay doi round khi tournament o trang thai Draft");
        }
        return tournament;
    }

    private Tournament ensureTournamentExists(Integer id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament"));
    }

    private Round getRoundOrThrow(Integer id) {
        return roundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay round"));
    }

    private void validateRequest(RoundRequest request, Tournament tournament) {
        if (request == null || request.getRoundName() == null || request.getRoundName().isBlank())
            throw new IllegalArgumentException("roundName khong duoc de trong");
        if (request.getRoundOrder() == null || request.getRoundOrder() <= 0)
            throw new IllegalArgumentException("roundOrder phai lon hon 0");
        if (request.getStartDate() != null && request.getStartDate().isBefore(tournament.getStartDate()))
            throw new IllegalArgumentException("startDate cua round nam ngoai tournament");
        if (request.getEndDate() != null && request.getEndDate().isAfter(tournament.getEndDate()))
            throw new IllegalArgumentException("endDate cua round nam ngoai tournament");
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate()))
            throw new IllegalArgumentException("endDate phai sau startDate");
    }

    private void requireOrganizer(User user) {
        if (user == null || user.getRole() == null || !"Organizer".equals(user.getRole().getRoleName()))
            throw new IllegalArgumentException("Chi Organizer moi co quyen thuc hien thao tac nay");
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
