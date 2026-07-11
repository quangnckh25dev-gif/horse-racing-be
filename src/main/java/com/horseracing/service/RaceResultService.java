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
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public RaceResultService(RaceResultRepository raceResultRepository,
                             RaceRepository raceRepository,
                             RaceEntryRepository raceEntryRepository,
                             RefereeRepository refereeRepository,
                             RaceRefereeRepository raceRefereeRepository,
                             TournamentRepository tournamentRepository,
                             NotificationRepository notificationRepository,
                             EntityManager entityManager) {
        this.raceResultRepository = raceResultRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.refereeRepository = refereeRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.tournamentRepository = tournamentRepository;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
    }

    public List<RaceResultResponse> getResultsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        return raceResultRepository.findByRaceId(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    //của buiquangann
    public RaceResultResponse createResult(Integer raceId, RaceResultRequest request, User refereeUser) {
        Referee referee = requireAssignedReferee(raceId, refereeUser);
        RaceEntry entry = getEligibleEntry(raceId, request == null ? null : request.getEntryId());

        raceResultRepository.findByRaceIdAndEntryId(raceId, entry.getEntryId())
                .ifPresent(result -> {
                    throw new IllegalArgumentException("Entry nay da co ket qua trong race");
                });

        RaceResult result = new RaceResult();
        result.setRaceId(raceId);
        result.setEntryId(entry.getEntryId());
        applyRefereeInput(result, request, referee);
        result.setApprovalStatus(STATUS_PENDING);
        result.setCreatedAt(LocalDateTime.now());

        return toResponse(raceResultRepository.save(result));
    }

    @Transactional
    public RaceResultResponse updateResult(Integer raceId, Integer resultId, RaceResultRequest request, User refereeUser) {
        Referee referee = requireAssignedReferee(raceId, refereeUser);
        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ket qua"));
        if (!result.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("Ket qua khong thuoc race nay");
        }
        if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
            throw new IllegalArgumentException("Ket qua da publish, khong the cap nhat");
        }

        RaceEntry entry = getEligibleEntry(raceId, request == null ? null : request.getEntryId());
        result.setEntryId(entry.getEntryId());
        applyRefereeInput(result, request, referee);
        result.setApprovalStatus(STATUS_PENDING);
        result.setApprovedByOrganizer(null);
        result.setApprovedAt(null);
        result.setPublishedAt(null);

        return toResponse(raceResultRepository.save(result));
    }

    @Transactional
    //của buiquangann
    public List<RaceResultResponse> approveResults(Integer raceId, User organizer) {
        ensureOwnedRace(raceId, organizer);
        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> {
            if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
                throw new IllegalArgumentException("Ket qua da publish, khong the duyet lai");
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
        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> {
            if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
                throw new IllegalArgumentException("Ket qua da publish, khong the tu choi");
            }
            result.setApprovalStatus(STATUS_REJECTED);
            result.setApprovedByOrganizer(organizer.getUserId());
            result.setApprovedAt(now);
            result.setPublishedAt(null);
        });
        notifyAssignedReferees(race, reason);

        return raceResultRepository.saveAll(results).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    //của buiquangann
    public List<RaceResultResponse> publishResults(Integer raceId, User organizer) {
        ensureOwnedRace(raceId, organizer);
        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        if (results.stream().anyMatch(result -> !STATUS_APPROVED.equals(result.getApprovalStatus()))) {
            throw new IllegalArgumentException("Ket qua chua duoc Organizer duyet, khong the publish");
        }

        entityManager.createNativeQuery("EXEC sp_PublishRaceResult @RaceID = :raceId")
                .setParameter("raceId", raceId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        return raceResultRepository.findByRaceId(raceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyRefereeInput(RaceResult result, RaceResultRequest request, Referee referee) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu ket qua khong duoc de trong");
        }
        boolean dnf = Boolean.TRUE.equals(request.getDnf());
        if (!dnf && (request.getFinishTime() == null || request.getFinishTime().isBlank())) {
            throw new IllegalArgumentException("finishTime la bat buoc neu horse khong DNF");
        }
        result.setFinishTime(dnf ? null : parseFinishTime(request.getFinishTime()));
        result.setDnf(dnf);
        result.setDq(false);
        result.setConfirmedByRef(referee.getRefereeId());
        result.setConfirmedAt(LocalDateTime.now());
    }

    private Referee requireAssignedReferee(Integer raceId, User user) {
        if (!isApprovedActiveRole(user, "Referee")) {
            throw new IllegalArgumentException("Chi Referee moi duoc nhap ket qua");
        }
        ensureRaceExists(raceId);
        Referee referee = refereeRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User chua co ho so Referee"));
        if (!raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, referee.getRefereeId())) {
            throw new IllegalArgumentException("Referee chua duoc phan cong vao race nay");
        }
        return referee;
    }

    private RaceEntry getEligibleEntry(Integer raceId, Integer entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId khong duoc de trong");
        }
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay entry"));
        if (!entry.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("Entry khong thuoc race nay");
        }
        if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())
                && !"Ready".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Chi nhap ket qua cho entry da duoc duyet");
        }
        return entry;
    }

    private Race ensureOwnedRace(Integer raceId, User organizer) {
        if (!isApprovedActiveRole(organizer, "Organizer")) {
            throw new IllegalArgumentException("Chi Organizer moi duoc xu ly ket qua race");
        }
        Race race = ensureRaceExists(raceId);
        tournamentRepository.findByTournamentIdAndCreatedBy(race.getTournamentId(), organizer.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Race khong thuoc Organizer hien tai"));
        return race;
    }

    private Race ensureRaceExists(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId khong hop le");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race"));
    }

    private List<RaceResult> getRaceResultsOrThrow(Integer raceId) {
        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Race chua co ket qua");
        }
        return results;
    }

    private void notifyAssignedReferees(Race race, String reason) {
        String body = reason == null || reason.isBlank()
                ? "Ket qua race " + race.getRaceName() + " bi tu choi. Vui long kiem tra lai."
                : reason.trim();
        raceRefereeRepository.findByRaceId(race.getRaceId()).forEach(assignment -> {
            refereeRepository.findById(assignment.getRefereeId()).ifPresent(referee -> {
                Notification notification = new Notification();
                notification.setUserId(referee.getUserId());
                notification.setTitle("Ket qua race bi tu choi");
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
            throw new IllegalArgumentException("finishTime phai co dang HH:mm:ss.SSS hoac so giay");
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
                calculatePoint(result.getFinishPosition()),
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

    private Integer calculatePoint(Integer position) {
        if (position == null) {
            return 0;
        }
        return switch (position) {
            case 1 -> 10;
            case 2 -> 7;
            case 3 -> 5;
            default -> 0;
        };
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
