package com.horseracing.service;

import com.horseracing.dto.RaceResultRequest;
import com.horseracing.dto.RaceResultResponse;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.RaceResult;
import com.horseracing.entity.Referee;
import com.horseracing.entity.Round;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceResultRepository;
import com.horseracing.repository.RefereeRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RaceResultService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_PUBLISHED = "Published";
    private static final String ENTRY_STATUS_READY = "Ready";
    private static final String READY_ENTRY_REQUIRED_MESSAGE = "Only ready entries with confirmed jockey can race.";

    private final RaceResultRepository raceResultRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RefereeRepository refereeRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final TournamentRepository tournamentRepository;
    private final NotificationRepository notificationRepository;
    private final RaceRankingService raceRankingService;
    private final HorseRepository horseRepository;
    private final HorseOwnerRepository horseOwnerRepository;
    private final WalletService walletService;
    private final BettingService bettingService;
    private final RoundRepository roundRepository;

    public RaceResultService(RaceResultRepository raceResultRepository,
                             RaceRepository raceRepository,
                             RaceEntryRepository raceEntryRepository,
                             RefereeRepository refereeRepository,
                             RaceRefereeRepository raceRefereeRepository,
                             TournamentRepository tournamentRepository,
                             NotificationRepository notificationRepository,
                             RaceRankingService raceRankingService,
                             HorseRepository horseRepository,
                             HorseOwnerRepository horseOwnerRepository,
                             WalletService walletService,
                             BettingService bettingService,
                             RoundRepository roundRepository) {
        this.raceResultRepository = raceResultRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.refereeRepository = refereeRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.tournamentRepository = tournamentRepository;
        this.notificationRepository = notificationRepository;
        this.raceRankingService = raceRankingService;
        this.horseRepository = horseRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.walletService = walletService;
        this.bettingService = bettingService;
        this.roundRepository = roundRepository;
    }

    @Transactional
    public List<RaceResultResponse> getResultsByRace(Integer raceId) {
        return getResultsByRace(raceId, null, null);
    }

    @Transactional
    public List<RaceResultResponse> getResultsByRace(Integer raceId, String status, String keyword) {
        ensureRaceExists(raceId);
        String statusFilter = normalizeOptionalApprovalStatus(status);
        String keywordFilter = trimToNull(keyword);
        raceRankingService.recalculateRace(raceId);
        return raceResultRepository.findByRaceId(raceId).stream()
                .filter(result -> statusFilter == null || statusFilter.equalsIgnoreCase(result.getApprovalStatus()))
                .filter(result -> matchesResultKeyword(result, keywordFilter))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RaceResultResponse> getPublishedResultsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        raceRankingService.recalculateRace(raceId);
        return raceResultRepository.findByRaceId(raceId).stream()
                .filter(result -> STATUS_PUBLISHED.equals(result.getApprovalStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RaceResultResponse createResult(Integer raceId, RaceResultRequest request, User refereeUser) {
        Referee referee = requireAssignedReferee(raceId, refereeUser);
        RaceEntry entry = getEligibleEntry(raceId, request == null ? null : request.getEntryId());

        raceResultRepository.findByRaceIdAndEntryId(raceId, entry.getEntryId())
                .ifPresent(result -> {
                    throw new IllegalArgumentException("This entry already has a result in this race.");
                });

        RaceResult result = new RaceResult();
        result.setRaceId(raceId);
        result.setEntryId(entry.getEntryId());
        applyRefereeInput(result, request, referee);
        result.setApprovalStatus(STATUS_PENDING);
        result.setCreatedAt(LocalDateTime.now());

        RaceResult saved = raceResultRepository.save(result);
        raceRankingService.recalculateRace(raceId);
        return toResponse(getResultOrThrow(saved.getResultId()));
    }

    @Transactional
    public RaceResultResponse updateResult(Integer raceId, Integer resultId, RaceResultRequest request, User refereeUser) {
        Referee referee = requireAssignedReferee(raceId, refereeUser);
        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Result was not found."));
        if (!result.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("Result does not belong to this race.");
        }
        if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
            throw new IllegalArgumentException("Published results cannot be updated.");
        }

        RaceEntry entry = getEligibleEntry(raceId, request == null ? null : request.getEntryId());
        result.setEntryId(entry.getEntryId());
        applyRefereeInput(result, request, referee);
        result.setApprovalStatus(STATUS_PENDING);
        result.setApprovedByOrganizer(null);
        result.setApprovedAt(null);
        result.setPublishedAt(null);

        RaceResult saved = raceResultRepository.save(result);
        raceRankingService.recalculateRace(raceId);
        return toResponse(getResultOrThrow(saved.getResultId()));
    }

    @Transactional
    public List<RaceResultResponse> approveResults(Integer raceId, User organizer) {
        ensureOwnedRace(raceId, organizer);
        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> {
            if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
                throw new IllegalArgumentException("Published results cannot be approved again.");
            }
            result.setApprovalStatus(STATUS_APPROVED);
            result.setApprovedByOrganizer(organizer.getUserId());
            result.setApprovedAt(now);
        });

        return raceResultRepository.saveAll(results).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RaceResultResponse> rejectResults(Integer raceId, String reason, User organizer) {
        Race race = ensureOwnedRace(raceId, organizer);
        String cleanReason = trimToNull(reason);
        if (cleanReason == null) {
            throw new IllegalArgumentException("Reject reason is required.");
        }
        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> {
            if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
                throw new IllegalArgumentException("Published results cannot be rejected.");
            }
            result.setApprovalStatus(STATUS_REJECTED);
            result.setApprovedByOrganizer(organizer.getUserId());
            result.setApprovedAt(now);
            result.setPublishedAt(null);
        });
        notifyAssignedReferees(race, cleanReason);

        return raceResultRepository.saveAll(results).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RaceResultResponse> publishResults(Integer raceId, User organizer) {
        ensureOwnedRace(raceId, organizer);
        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        if (results.stream().anyMatch(result -> !STATUS_APPROVED.equals(result.getApprovalStatus()))) {
            throw new IllegalArgumentException("Results cannot be published before Organizer approval.");
        }

        raceResultRepository.publishRaceResult(raceId, organizer.getUserId());
        applyRoundAdvancement(raceId);
        awardOwnerPrizes(raceId);
        bettingService.settleRaceBets(raceId);
        return getPublishedResultsByRace(raceId);
    }

    private void applyRefereeInput(RaceResult result, RaceResultRequest request, Referee referee) {
        if (request == null) {
            throw new IllegalArgumentException("Result data is required.");
        }
        boolean dnf = Boolean.TRUE.equals(request.getDnf());
        if (!dnf && (request.getFinishTime() == null || request.getFinishTime().isBlank())) {
            throw new IllegalArgumentException("finishTime is required when horse is not DNF.");
        }
        result.setFinishTime(dnf ? null : parseFinishTime(request.getFinishTime()));
        result.setPenaltyTime(defaultDecimal(result.getPenaltyTime()));
        result.setPoints(result.getPoints() == null ? 0 : result.getPoints());
        result.setPrizeWon(defaultDecimal(result.getPrizeWon()));
        result.setDnf(dnf);
        result.setDq(false);
        result.setConfirmedByRef(referee.getRefereeId());
        result.setConfirmedAt(LocalDateTime.now());
    }

    private Referee requireAssignedReferee(Integer raceId, User user) {
        if (!isApprovedActiveRole(user, "Referee")) {
            throw new IllegalArgumentException("Only referees can submit results.");
        }
        ensureRaceExists(raceId);
        Referee referee = refereeRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User does not have a Referee profile."));
        if (!raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, referee.getRefereeId())) {
            throw new IllegalArgumentException("Referee has not been assigned to this race.");
        }
        return referee;
    }

    private RaceEntry getEligibleEntry(Integer raceId, Integer entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId is required.");
        }
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry was not found."));
        if (!entry.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("This entry does not belong to the selected race.");
        }
        if (!isReadyWithConfirmedJockey(entry)) {
            throw new IllegalArgumentException(READY_ENTRY_REQUIRED_MESSAGE);
        }
        return entry;
    }

    private boolean isReadyWithConfirmedJockey(RaceEntry entry) {
        return entry != null
                && ENTRY_STATUS_READY.equalsIgnoreCase(entry.getRegistrationStatus())
                && entry.getJockeyId() != null
                && Boolean.TRUE.equals(entry.getJockeyConfirmed());
    }

    private Race ensureOwnedRace(Integer raceId, User organizer) {
        if (!isApprovedActiveRole(organizer, "Organizer")) {
            throw new IllegalArgumentException("Only organizers can process race results.");
        }
        Race race = ensureRaceExists(raceId);
        tournamentRepository.findByTournamentIdAndCreatedBy(race.getTournamentId(), organizer.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Race does not belong to the current organizer."));
        return race;
    }

    private Race ensureRaceExists(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId is invalid.");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race was not found."));
    }

    private List<RaceResult> getRaceResultsOrThrow(Integer raceId) {
        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Race does not have results yet.");
        }
        return results;
    }

    private void applyRoundAdvancement(Integer raceId) {
        Race race = ensureRaceExists(raceId);
        if (race.getRoundId() == null) {
            return;
        }

        Round round = roundRepository.findById(race.getRoundId())
                .orElseThrow(() -> new IllegalArgumentException("Round was not found."));
        Integer roundOrder = round.getRoundOrder();
        if (roundOrder == null) {
            return;
        }

        raceRankingService.recalculateRace(raceId);
        List<RaceEntry> entries = raceEntryRepository.findAdvancementEligibleEntriesByRaceId(raceId);
        if (entries.isEmpty()) {
            return;
        }

        Map<Integer, RaceResult> resultByEntryId = raceResultRepository.findByRaceId(raceId).stream()
                .filter(result -> STATUS_PUBLISHED.equals(result.getApprovalStatus()))
                .collect(Collectors.toMap(RaceResult::getEntryId, Function.identity(), (left, right) -> left));

        List<RaceEntry> orderedEntries = entries.stream()
                .sorted(Comparator
                        .comparing((RaceEntry entry) -> isRankedResult(resultByEntryId.get(entry.getEntryId())) ? 0 : 1)
                        .thenComparing(entry -> {
                            RaceResult result = resultByEntryId.get(entry.getEntryId());
                            return result == null || result.getFinishPosition() == null
                                    ? Integer.MAX_VALUE
                                    : result.getFinishPosition();
                        })
                        .thenComparing(RaceEntry::getEntryId))
                .toList();

        if (roundOrder >= 3) {
            orderedEntries.forEach(entry -> {
                entry.setRoundStatus("Finalist");
                entry.setEliminationRoundId(null);
                entry.setEliminationReason(null);
            });
            raceEntryRepository.saveAll(orderedEntries);
            return;
        }

        int eliminationCount = (int) Math.ceil(orderedEntries.size() / 4.0);
        int qualifiedCount = Math.max(0, orderedEntries.size() - eliminationCount);
        List<RaceEntry> qualifiedEntries = orderedEntries.subList(0, qualifiedCount);
        List<RaceEntry> eliminatedEntries = orderedEntries.subList(qualifiedCount, orderedEntries.size());

        qualifiedEntries.forEach(entry -> {
            entry.setRoundStatus("Qualified");
            entry.setEliminationRoundId(null);
            entry.setEliminationReason(null);
        });
        eliminatedEntries.forEach(entry -> {
            entry.setRoundStatus("Eliminated");
            entry.setEliminationRoundId(round.getRoundId());
            entry.setEliminationReason(round.getRoundName() + " cutoff");
        });
        raceEntryRepository.saveAll(orderedEntries);
        createNextRoundEntries(race, roundOrder + 1, qualifiedEntries);
    }

    private boolean isRankedResult(RaceResult result) {
        return result != null
                && result.getFinishPosition() != null
                && !Boolean.TRUE.equals(result.getDnf())
                && !Boolean.TRUE.equals(result.getDq());
    }

    private void createNextRoundEntries(Race currentRace, Integer nextRoundOrder, List<RaceEntry> qualifiedEntries) {
        roundRepository.findByTournamentIdAndRoundOrder(currentRace.getTournamentId(), nextRoundOrder)
                .flatMap(nextRound -> raceRepository.findByTournamentIdAndRoundId(
                                currentRace.getTournamentId(), nextRound.getRoundId())
                        .stream()
                        .findFirst())
                .ifPresent(nextRace -> qualifiedEntries.forEach(entry -> {
                    if (raceEntryRepository.existsByRaceIdAndHorseId(nextRace.getRaceId(), entry.getHorseId())) {
                        return;
                    }
                    RaceEntry nextEntry = new RaceEntry();
                    nextEntry.setRaceId(nextRace.getRaceId());
                    nextEntry.setHorseId(entry.getHorseId());
                    nextEntry.setJockeyId(entry.getJockeyId());
                    nextEntry.setLaneNumber(entry.getLaneNumber());
                    nextEntry.setRegistrationStatus(entry.getRegistrationStatus());
                    nextEntry.setOrganizerApproved(entry.getOrganizerApproved());
                    nextEntry.setApprovedBy(entry.getApprovedBy());
                    nextEntry.setRejectReason(null);
                    nextEntry.setRoundStatus(null);
                    nextEntry.setEliminationRoundId(null);
                    nextEntry.setEliminationReason(null);
                    nextEntry.setJockeyConfirmed(entry.getJockeyConfirmed());
                    nextEntry.setOdds(entry.getOdds());
                    raceEntryRepository.save(nextEntry);
                }));
    }

    private RaceResult getResultOrThrow(Integer resultId) {
        return raceResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Result was not found."));
    }

    private void notifyAssignedReferees(Race race, String reason) {
        String body = "Race results were rejected. Reason: " + reason;
        raceRefereeRepository.findByRaceId(race.getRaceId()).forEach(assignment -> {
            refereeRepository.findById(assignment.getRefereeId()).ifPresent(referee -> {
                Notification notification = new Notification();
                notification.setUserId(referee.getUserId());
                notification.setTitle("Race results rejected");
                notification.setBody(body);
                notification.setNotifType("ResultRejected");
                notification.setRelatedEntity("Race");
                notification.setRelatedEntityId(race.getRaceId());
                notification.setIsRead(false);
                notificationRepository.save(notification);
            });
        });
    }

    private void awardOwnerPrizes(Integer raceId) {
        raceResultRepository.findByRaceId(raceId).stream()
                .filter(result -> STATUS_PUBLISHED.equals(result.getApprovalStatus()))
                .filter(result -> defaultDecimal(result.getPrizeWon()).compareTo(BigDecimal.ZERO) > 0)
                .forEach(result -> {
                    RaceEntry entry = raceEntryRepository.findById(result.getEntryId())
                            .orElseThrow(() -> new IllegalArgumentException("Race entry was not found for prize payout."));
                    Integer ownerId = horseRepository.findById(entry.getHorseId())
                            .orElseThrow(() -> new IllegalArgumentException("Horse was not found for prize payout."))
                            .getOwnerId();
                    Integer ownerUserId = horseOwnerRepository.findById(ownerId)
                            .orElseThrow(() -> new IllegalArgumentException("Horse owner was not found for prize payout."))
                            .getUserId();
                    walletService.creditPrizeAward(ownerUserId, result.getPrizeWon(), raceId, result.getResultId());
                });
    }

    private boolean isApprovedActiveRole(User user, String roleName) {
        return user != null
                && user.getRole() != null
                && roleName.equalsIgnoreCase(user.getRole().getRoleName())
                && Boolean.TRUE.equals(user.getIsActive())
                && Boolean.TRUE.equals(user.getIsApproved());
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptionalApprovalStatus(String status) {
        String cleanStatus = trimToNull(status);
        if (cleanStatus == null) {
            return null;
        }
        if (!STATUS_PENDING.equalsIgnoreCase(cleanStatus)
                && !STATUS_APPROVED.equalsIgnoreCase(cleanStatus)
                && !STATUS_REJECTED.equalsIgnoreCase(cleanStatus)
                && !STATUS_PUBLISHED.equalsIgnoreCase(cleanStatus)) {
            throw new IllegalArgumentException("status only accepts Pending, Approved, Rejected, or Published.");
        }
        return cleanStatus;
    }

    private boolean matchesResultKeyword(RaceResult result, String keyword) {
        if (keyword == null) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(raceResultRepository.findHorseNameByEntryId(result.getEntryId()), normalized)
                || contains(raceResultRepository.findJockeyNameByEntryId(result.getEntryId()), normalized)
                || contains(String.valueOf(result.getFinishPosition()), normalized)
                || contains(result.getApprovalStatus(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private BigDecimal parseFinishTime(String finishTime) {
        String[] parts = finishTime.split(":");
        try {
            if (parts.length == 3) {
                BigDecimal hours = new BigDecimal(parts[0]).multiply(BigDecimal.valueOf(3600));
                BigDecimal minutes = new BigDecimal(parts[1]).multiply(BigDecimal.valueOf(60));
                BigDecimal seconds = new BigDecimal(parts[2]);
                return hours.add(minutes).add(seconds).setScale(3, RoundingMode.HALF_UP);
            }
            return new BigDecimal(finishTime).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("finishTime must be in HH:mm:ss.SSS format or a number of seconds.");
        }
    }

    private RaceResultResponse toResponse(RaceResult result) {
        return new RaceResultResponse(
                result.getResultId(),
                result.getRaceId(),
                result.getEntryId(),
                raceResultRepository.findHorseNameByEntryId(result.getEntryId()),
                raceResultRepository.findJockeyNameByEntryId(result.getEntryId()),
                result.getFinishPosition(),
                formatFinishTime(result.getFinishTime()),
                formatFinishTime(result.getPenaltyTime()),
                formatFinishTime(result.getFinalTime()),
                result.getPoints(),
                result.getPrizeWon(),
                result.getDnf(),
                result.getDq(),
                result.getConfirmedByRef(),
                result.getConfirmedAt(),
                result.getApprovalStatus(),
                result.getApprovedByOrganizer(),
                result.getApprovedAt(),
                result.getPublishedAt(),
                STATUS_PUBLISHED.equals(result.getApprovalStatus()),
                result.getCreatedAt()
        );
    }

    private String formatFinishTime(BigDecimal finishTime) {
        if (finishTime == null) {
            return null;
        }
        BigDecimal scaled = finishTime.setScale(3, RoundingMode.HALF_UP);
        int totalSeconds = scaled.intValue();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        BigDecimal seconds = scaled.subtract(BigDecimal.valueOf(hours * 3600L + minutes * 60L));
        return "%02d:%02d:%06.3f".formatted(hours, minutes, seconds);
    }
}
