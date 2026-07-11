package com.horseracing.service;

import com.horseracing.dto.RaceResultRequest;
import com.horseracing.dto.RaceResultResponse;
import com.horseracing.entity.RaceResult;
import com.horseracing.entity.User;
import com.horseracing.repository.RaceResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RaceResultService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_PUBLISHED = "Published";

    private final RaceResultRepository raceResultRepository;

    public RaceResultService(RaceResultRepository raceResultRepository) {
        this.raceResultRepository = raceResultRepository;
    }

    public List<RaceResultResponse> getResultsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        return raceResultRepository.findByRaceId(raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceResultResponse createResult(Integer raceId, RaceResultRequest request, User currentUser) {
        ensureRaceExists(raceId);
        Integer refereeId = ensureAssignedReferee(raceId, currentUser);
        validateResultRequest(request);
        ensureEntryBelongsToRace(raceId, request.getEntryId());

        raceResultRepository.findByRaceIdAndEntryId(raceId, request.getEntryId())
                .ifPresent(result -> {
                    throw new IllegalArgumentException("Entry nay da co ket qua trong race");
                });

        RaceResult result = new RaceResult();
        result.setRaceId(raceId);
        applyResultRequest(result, request, refereeId);
        result.setApprovalStatus(STATUS_PENDING);
        result.setCreatedAt(LocalDateTime.now());

        return toResponse(raceResultRepository.save(result));
    }

    public RaceResultResponse updateResult(Integer raceId, Integer resultId, RaceResultRequest request, User currentUser) {
        ensureRaceExists(raceId);
        Integer refereeId = ensureAssignedReferee(raceId, currentUser);
        validateResultRequest(request);

        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ket qua"));

        if (!result.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("Ket qua khong thuoc race nay");
        }

        if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
            throw new IllegalArgumentException("Ket qua da public, khong the cap nhat");
        }

        ensureEntryBelongsToRace(raceId, request.getEntryId());
        applyResultRequest(result, request, refereeId);
        result.setApprovalStatus(STATUS_PENDING);
        result.setApprovedByOrganizer(null);
        result.setApprovedAt(null);
        result.setPublishedAt(null);

        return toResponse(raceResultRepository.save(result));
    }

    public List<RaceResultResponse> approveResults(Integer raceId, User currentUser) {
        ensureRaceExists(raceId);
        Integer organizerId = ensureOrganizer(currentUser);

        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> {
            if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
                throw new IllegalArgumentException("Ket qua da public, khong the duyet lai");
            }
            result.setApprovalStatus(STATUS_APPROVED);
            result.setApprovedByOrganizer(organizerId);
            result.setApprovedAt(now);
        });

        return raceResultRepository.saveAll(results)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RaceResultResponse> rejectResults(Integer raceId, User currentUser) {
        ensureRaceExists(raceId);
        Integer organizerId = ensureOrganizer(currentUser);

        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> {
            if (STATUS_PUBLISHED.equals(result.getApprovalStatus())) {
                throw new IllegalArgumentException("Ket qua da public, khong the tu choi");
            }
            result.setApprovalStatus(STATUS_REJECTED);
            result.setApprovedByOrganizer(organizerId);
            result.setApprovedAt(now);
            result.setPublishedAt(null);
        });

        return raceResultRepository.saveAll(results)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RaceResultResponse> publishResults(Integer raceId, User currentUser) {
        ensureRaceExists(raceId);
        Integer organizerId = ensureOrganizer(currentUser);

        List<RaceResult> results = getRaceResultsOrThrow(raceId);
        boolean hasUnapprovedResult = results.stream()
                .anyMatch(result -> !STATUS_APPROVED.equals(result.getApprovalStatus()));

        if (hasUnapprovedResult) {
            throw new IllegalArgumentException("Ket qua chua duoc Ban to chuc duyet, khong the public");
        }

        raceResultRepository.publishRaceResult(raceId, organizerId);

        return raceResultRepository.findByRaceId(raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyResultRequest(RaceResult result, RaceResultRequest request, Integer refereeId) {
        result.setEntryId(request.getEntryId());
        result.setFinishTime(parseFinishTime(request.getFinishTime()));
        result.setPenaltyTime(defaultDecimal(result.getPenaltyTime()));
        result.setPoints(result.getPoints() == null ? 0 : result.getPoints());
        result.setPrizeWon(defaultDecimal(result.getPrizeWon()));
        result.setDnf(Boolean.TRUE.equals(request.getDnf()));
        result.setDq(Boolean.TRUE.equals(result.getDq()));
        result.setConfirmedByRef(refereeId);
        result.setConfirmedAt(LocalDateTime.now());
    }

    private List<RaceResult> getRaceResultsOrThrow(Integer raceId) {
        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Race chua co ket qua");
        }
        return results;
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || raceResultRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Khong tim thay race");
        }
    }

    private void ensureEntryBelongsToRace(Integer raceId, Integer entryId) {
        if (entryId == null || raceResultRepository.countEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("Entry khong thuoc race nay");
        }
    }

    private Integer ensureOrganizer(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Thieu thong tin Organizer hien tai");
        }
        Integer organizerId = currentUser.getUserId();
        if (organizerId == null || raceResultRepository.countOrganizerById(organizerId) == 0) {
            throw new IllegalArgumentException("Khong tim thay Ban to chuc hop le");
        }
        return organizerId;
    }

    private Integer ensureAssignedReferee(Integer raceId, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null
                || !"Referee".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            throw new IllegalArgumentException("Chi Referee moi duoc nhap ket qua race");
        }
        if (raceResultRepository.countAssignedReferee(raceId, currentUser.getUserId()) == 0) {
            throw new IllegalArgumentException("Referee chua duoc phan cong cho race nay");
        }
        Integer refereeId = raceResultRepository.findRefereeIdByUserId(currentUser.getUserId());
        if (refereeId == null) {
            throw new IllegalArgumentException("User hien tai chua co ho so Referee");
        }
        return refereeId;
    }

    private void validateResultRequest(RaceResultRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu ket qua khong hop le");
        }
        if (request.getEntryId() == null) {
            throw new IllegalArgumentException("entryId khong duoc de trong");
        }
        if (!Boolean.TRUE.equals(request.getDnf())
                && (request.getFinishTime() == null || request.getFinishTime().isBlank())) {
            throw new IllegalArgumentException("finishTime khong duoc de trong neu ngua khong DNF");
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal parseFinishTime(String finishTime) {
        if (finishTime == null || finishTime.isBlank()) {
            return null;
        }

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
