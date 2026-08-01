package com.horseracing.service;

import com.horseracing.dto.RaceSummaryResponse;
import com.horseracing.dto.RoundSummaryResponse;
import com.horseracing.dto.TournamentDetailResponse;
import com.horseracing.dto.TournamentRequest;
import com.horseracing.dto.TournamentResponse;
import com.horseracing.entity.Race;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TournamentService {

    private static final String DRAFT = "Draft";
    private static final Set<Integer> ALLOWED_MAX_HORSES = Set.of(8, 12, 16);
    private static final Set<String> PUBLIC_STATUSES = Set.of("Open", "Ongoing", "Finished", "Cancelled");
    private static final List<DefaultRound> DEFAULT_ROUNDS = List.of(
            new DefaultRound("Qualify", 1),
            new DefaultRound("Semi Final", 2),
            new DefaultRound("Final", 3)
    );

    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;
    private final RaceRepository raceRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             RoundRepository roundRepository,
                             RaceRepository raceRepository) {
        this.tournamentRepository = tournamentRepository;
        this.roundRepository = roundRepository;
        this.raceRepository = raceRepository;
    }

    public List<TournamentResponse> getPublicTournaments() {
        return tournamentRepository.findAll().stream()
                .filter(tournament -> PUBLIC_STATUSES.contains(tournament.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    public List<TournamentResponse> getAdminTournaments(String status) {
        List<Tournament> tournaments = status == null || status.isBlank()
                ? tournamentRepository.findAll()
                : tournamentRepository.findByStatus(status.trim());
        return tournaments.stream().map(this::toResponse).toList();
    }

    public List<TournamentResponse> getMyTournaments(User organizer) {
        requireOrganizer(organizer);
        return tournamentRepository.findByCreatedByOrderByCreatedAtDesc(organizer.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TournamentDetailResponse getTournamentDetail(Integer tournamentId) {
        Tournament tournament = getTournamentOrThrow(tournamentId);
        return buildDetail(tournament);
    }

    public TournamentDetailResponse getOwnedTournamentDetail(Integer tournamentId, User organizer) {
        requireOrganizer(organizer);
        return buildDetail(getOwnedTournamentOrThrow(tournamentId, organizer.getUserId()));
    }

    public List<RoundSummaryResponse> getTournamentRounds(Integer tournamentId) {
        getTournamentOrThrow(tournamentId);
        return roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId).stream()
                .map(this::toRoundSummary)
                .toList();
    }

    @Transactional
    //của buiquangann
    public TournamentResponse createTournament(TournamentRequest request, User organizer) {
        requireOrganizer(organizer);
        validateTournamentRequest(request);

        Tournament tournament = new Tournament();
        applyRequest(tournament, request);
        tournament.setStatus(DRAFT);
        tournament.setCreatedBy(organizer.getUserId());
        Tournament saved = tournamentRepository.save(tournament);
        ensureDefaultRounds(saved);
        return toResponse(saved);
    }

    @Transactional
    public TournamentResponse updateTournament(Integer tournamentId, TournamentRequest request, User organizer) {
        requireOrganizer(organizer);
        validateTournamentRequest(request);
        Tournament tournament = getOwnedTournamentOrThrow(tournamentId, organizer.getUserId());
        ensureDraft(tournament, "Only Draft tournaments can be updated.");

        applyRequest(tournament, request);
        tournament.setRejectReason(null);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    //của buiquangann
    public TournamentResponse submitTournament(Integer tournamentId, User organizer) {
        requireOrganizer(organizer);
        Tournament tournament = getOwnedTournamentOrThrow(tournamentId, organizer.getUserId());
        return toResponse(tournament);
    }

    @Transactional
    public TournamentResponse reviewTournament(Integer tournamentId, String requestedStatus,
                                               String reason, User admin) {
        requireAdmin(admin);
        getTournamentOrThrow(tournamentId);
        throw new IllegalArgumentException("Admin tournament approval is no longer supported.");
    }

    private TournamentDetailResponse buildDetail(Tournament tournament) {
        Integer tournamentId = tournament.getTournamentId();
        List<RoundSummaryResponse> rounds = roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId)
                .stream().map(this::toRoundSummary).toList();
        List<RaceSummaryResponse> races = raceRepository.findByTournamentId(tournamentId)
                .stream().map(this::toRaceSummary).toList();
        return new TournamentDetailResponse(toResponse(tournament), rounds, races);
    }

    private Tournament getTournamentOrThrow(Integer tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is invalid.");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament was not found."));
    }

    private Tournament getOwnedTournamentOrThrow(Integer tournamentId, Integer organizerId) {
        return tournamentRepository.findByTournamentIdAndCreatedBy(tournamentId, organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament was not found for the current organizer."));
    }

    private void validateTournamentRequest(TournamentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Tournament data is required.");
        }
        if (request.getTournamentName() == null || request.getTournamentName().isBlank()) {
            throw new IllegalArgumentException("tournamentName is required.");
        }
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            throw new IllegalArgumentException("location is required.");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required.");
        }
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("endDate is required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate.");
        }
        if (request.getBudgetTotal() != null && request.getBudgetTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("budgetTotal cannot be negative.");
        }
        if (!ALLOWED_MAX_HORSES.contains(request.getMaxHorses())) {
            throw new IllegalArgumentException("maxHorses only accepts 8, 12, or 16.");
        }
        if (request.getMaxParticipants() != null && request.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("maxParticipants must be greater than 0.");
        }
    }

    private void ensureDraft(Tournament tournament, String message) {
        if (!DRAFT.equals(tournament.getStatus())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireOrganizer(User user) {
        requireRole(user, "Organizer");
    }

    private void requireAdmin(User user) {
        requireRole(user, "Admin");
    }

    private void requireRole(User user, String role) {
        if (user == null || user.getRole() == null || !role.equals(user.getRole().getRoleName())) {
            throw new IllegalArgumentException("You do not have permission to perform this action.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Account is inactive or has not been approved.");
        }
    }

    private void applyRequest(Tournament tournament, TournamentRequest request) {
        tournament.setTournamentName(request.getTournamentName().trim());
        tournament.setDescription(trimToNull(request.getDescription()));
        tournament.setLocation(trimToNull(request.getLocation()));
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setBudgetTotal(request.getBudgetTotal() == null ? BigDecimal.ZERO : request.getBudgetTotal());
        tournament.setMaxHorses(request.getMaxHorses());
        tournament.setMaxParticipants(request.getMaxParticipants());
    }

    private void ensureDefaultRounds(Tournament tournament) {
        List<Round> existingRounds = roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournament.getTournamentId());
        if (!existingRounds.isEmpty()) {
            return;
        }
        DEFAULT_ROUNDS.forEach(defaultRound -> {
            Round round = new Round();
            round.setTournamentId(tournament.getTournamentId());
            round.setRoundName(defaultRound.name());
            round.setRoundOrder(defaultRound.order());
            round.setStartDate(tournament.getStartDate());
            round.setEndDate(tournament.getEndDate());
            roundRepository.save(round);
        });
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getTournamentId(), tournament.getTournamentName(), tournament.getDescription(),
                tournament.getLocation(), tournament.getStartDate(), tournament.getEndDate(),
                tournament.getBudgetTotal(), tournament.getMaxHorses(), tournament.getMaxParticipants(),
                tournament.getStatus(), tournament.getCreatedBy(), tournament.getApprovedByAdmin(),
                tournament.getApprovedAt(), tournament.getRejectReason(), tournament.getCreatedAt(),
                tournament.getUpdatedAt()
        );
    }

    private RoundSummaryResponse toRoundSummary(Round round) {
        return new RoundSummaryResponse(round.getRoundId(), round.getTournamentId(), round.getRoundName(),
                round.getRoundOrder(), round.getStartDate(), round.getEndDate(), round.getDescription(),
                round.getCreatedAt());
    }

    private RaceSummaryResponse toRaceSummary(Race race) {
        return new RaceSummaryResponse(race.getRaceId(), race.getTournamentId(), race.getRoundId(),
                race.getRaceName(), race.getRaceDate(), race.getTrackLength(), race.getTrackType(),
                race.getMaxParticipants(), race.getPrizeFirst(), race.getPrizeSecond(), race.getPrizeThird(),
                race.getStatus(), race.getRegistrationOpen(), race.getRegistrationClose());
    }

    private record DefaultRound(String name, Integer order) {
    }
}
