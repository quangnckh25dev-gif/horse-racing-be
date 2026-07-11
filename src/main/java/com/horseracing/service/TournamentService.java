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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TournamentService {

    private static final String DRAFT = "Draft";
    private static final String PENDING_APPROVAL = "PendingApproval";
    private static final Set<String> PUBLIC_STATUSES = Set.of("Open", "Ongoing", "Finished", "Cancelled");

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
        validateDates(request);

        Tournament tournament = new Tournament();
        applyRequest(tournament, request);
        tournament.setStatus(DRAFT);
        tournament.setCreatedBy(organizer.getUserId());
        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse updateTournament(Integer tournamentId, TournamentRequest request, User organizer) {
        requireOrganizer(organizer);
        validateDates(request);
        Tournament tournament = getOwnedTournamentOrThrow(tournamentId, organizer.getUserId());
        ensureDraft(tournament, "Chi co the cap nhat giai dau dang o trang thai Draft");

        applyRequest(tournament, request);
        tournament.setRejectReason(null);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    //của buiquangann
    public TournamentResponse submitTournament(Integer tournamentId, User organizer) {
        requireOrganizer(organizer);
        Tournament tournament = getOwnedTournamentOrThrow(tournamentId, organizer.getUserId());
        ensureDraft(tournament, "Chi co the gui duyet giai dau dang o trang thai Draft");
        tournament.setStatus(PENDING_APPROVAL);
        tournament.setRejectReason(null);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse reviewTournament(Integer tournamentId, String requestedStatus,
                                               String reason, User admin) {
        requireAdmin(admin);
        Tournament tournament = getTournamentOrThrow(tournamentId);
        if (!PENDING_APPROVAL.equals(tournament.getStatus())) {
            throw new IllegalArgumentException("Chi co the duyet giai dau dang cho phe duyet");
        }
        if ("Open".equals(requestedStatus)) {
            tournament.setStatus("Open");
            tournament.setApprovedByAdmin(admin.getUserId());
            tournament.setApprovedAt(LocalDateTime.now());
            tournament.setRejectReason(null);
        } else if (DRAFT.equals(requestedStatus)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason la bat buoc khi tu choi giai dau");
            }
            tournament.setStatus(DRAFT);
            tournament.setApprovedByAdmin(null);
            tournament.setApprovedAt(null);
            tournament.setRejectReason(reason.trim());
        } else {
            throw new IllegalArgumentException("Admin chi duoc chuyen trang thai sang Open hoac Draft");
        }
        return toResponse(tournamentRepository.save(tournament));
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
            throw new IllegalArgumentException("tournamentId khong hop le");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament"));
    }

    private Tournament getOwnedTournamentOrThrow(Integer tournamentId, Integer organizerId) {
        return tournamentRepository.findByTournamentIdAndCreatedBy(tournamentId, organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament thuoc Organizer hien tai"));
    }

    private void validateDates(TournamentRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate phai lon hon hoac bang startDate");
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
            throw new IllegalArgumentException("Ban khong co quyen thuc hien thao tac nay");
        }
        if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Tai khoan chua duoc kich hoat hoac phe duyet");
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
}
