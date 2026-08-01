package com.horseracing.service;

import com.horseracing.dto.RaceResultRequest;
import com.horseracing.dto.RaceResultResponse;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.RaceResult;
import com.horseracing.entity.Referee;
import com.horseracing.entity.User;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceResultRepository;
import com.horseracing.repository.RefereeRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RaceResultService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_PUBLISHED = "Published";

    private final RaceResultRepository raceResultRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RefereeRepository refereeRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final TournamentRepository tournamentRepository;
    private final NotificationRepository notificationRepository;
    private final RaceRankingService raceRankingService;

    public RaceResultService(RaceResultRepository raceResultRepository,
                             RaceRepository raceRepository,
                             RaceEntryRepository raceEntryRepository,
                             RefereeRepository refereeRepository,
                             RaceRefereeRepository raceRefereeRepository,
                             TournamentRepository tournamentRepository,
                             NotificationRepository notificationRepository,
                             RaceRankingService raceRankingService) {
        this.raceResultRepository = raceResultRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.refereeRepository = refereeRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.tournamentRepository = tournamentRepository;
        this.notificationRepository = notificationRepository;
        this.raceRankingService = raceRankingService;
    }

    @Transactional
    public List<RaceResultResponse> getResultsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        raceRankingService.recalculateRace(raceId);
        return raceResultRepository.findByRaceId(raceId).stream()
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
        if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())
                && !"Ready".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Results can only be submitted for approved entries.");
        }
        return entry;
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
