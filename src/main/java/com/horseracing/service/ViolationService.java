package com.horseracing.service;

import com.horseracing.dto.ViolationOptionResponse;
import com.horseracing.dto.ViolationRequest;
import com.horseracing.dto.ViolationResponse;
import com.horseracing.entity.User;
import com.horseracing.entity.Violation;
import com.horseracing.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ViolationService {

    private static final Map<String, ViolationRule> RULE_BY_VIOLATION = new LinkedHashMap<>();

    static {
        RULE_BY_VIOLATION.put("FalseStart", new ViolationRule("False Start", BigDecimal.valueOf(3), false));
        RULE_BY_VIOLATION.put("LaneViolation", new ViolationRule("Lane Violation", BigDecimal.valueOf(5), false));
        RULE_BY_VIOLATION.put("Obstruction", new ViolationRule("Obstruction", BigDecimal.valueOf(10), false));
        RULE_BY_VIOLATION.put("SeriousViolation", new ViolationRule("Serious Violation", BigDecimal.ZERO, true));
    }

    private final ViolationRepository violationRepository;
    private final RaceRankingService raceRankingService;

    public ViolationService(ViolationRepository violationRepository,
                            RaceRankingService raceRankingService) {
        this.violationRepository = violationRepository;
        this.raceRankingService = raceRankingService;
    }

    public List<ViolationResponse> getViolationsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        return violationRepository.findByRaceId(raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ViolationOptionResponse> getViolationOptions() {
        return RULE_BY_VIOLATION.entrySet()
                .stream()
                .map(entry -> new ViolationOptionResponse(
                        entry.getKey(),
                        entry.getValue().label(),
                        entry.getValue().penaltySeconds(),
                        entry.getValue().isDq()
                ))
                .toList();
    }

    public ViolationResponse createViolation(Integer raceId, ViolationRequest request, User currentUser) {
        ensureRaceExists(raceId);
        Integer refereeId = ensureAssignedReferee(raceId, currentUser);
        validateRequest(request);
        ensureEntryBelongsToRace(raceId, request.getEntryId());
        String violationType = normalizeViolationType(request.getViolationType());
        ViolationRule rule = RULE_BY_VIOLATION.get(violationType);

        Violation violation = new Violation();
        violation.setRaceId(raceId);
        violation.setEntryId(request.getEntryId());
        violation.setRefereeId(refereeId);
        violation.setViolationType(violationType);
        violation.setPenaltySeconds(rule.penaltySeconds());
        violation.setIsDq(rule.isDq());
        violation.setEvidenceImageUrl(trimToNull(request.getEvidenceImageUrl()));
        violation.setDescription(trimToNull(request.getDescription()));
        violation.setRecordedAt(LocalDateTime.now());

        Violation saved = violationRepository.save(violation);
        raceRankingService.recalculateRace(raceId);
        return toResponse(saved);
    }

    public ViolationResponse updateViolation(Integer violationId, ViolationRequest request, User currentUser) {
        if (request == null) {
            throw new IllegalArgumentException("Violation data is invalid.");
        }
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("Violation was not found."));
        Integer refereeId = ensureAssignedReferee(violation.getRaceId(), currentUser);
        violation.setRefereeId(refereeId);

        if (request.getEntryId() != null) {
            ensureEntryBelongsToRace(violation.getRaceId(), request.getEntryId());
            violation.setEntryId(request.getEntryId());
        }

        if (request.getViolationType() != null) {
            String violationType = normalizeViolationType(request.getViolationType());
            ViolationRule rule = RULE_BY_VIOLATION.get(violationType);
            violation.setViolationType(violationType);
            violation.setPenaltySeconds(rule.penaltySeconds());
            violation.setIsDq(rule.isDq());
        }

        if (request.getEvidenceImageUrl() != null) {
            violation.setEvidenceImageUrl(trimToNull(request.getEvidenceImageUrl()));
        }
        if (request.getDescription() != null) {
            violation.setDescription(trimToNull(request.getDescription()));
        }

        Violation saved = violationRepository.save(violation);
        raceRankingService.recalculateRace(saved.getRaceId());
        return toResponse(saved);
    }

    public void deleteViolation(Integer violationId, User currentUser) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("Violation was not found."));
        ensureAssignedReferee(violation.getRaceId(), currentUser);
        Integer raceId = violation.getRaceId();
        violationRepository.deleteById(violationId);
        raceRankingService.recalculateRace(raceId);
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || violationRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Race was not found.");
        }
    }

    private void ensureEntryBelongsToRace(Integer raceId, Integer entryId) {
        if (entryId == null || violationRepository.countEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("This entry does not belong to the selected race.");
        }
        if (violationRepository.countEligibleEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("Violations can only be recorded for approved race entries.");
        }
    }

    private Integer ensureAssignedReferee(Integer raceId, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null
                || !"Referee".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            throw new IllegalArgumentException("Only referees can record violations.");
        }
        if (violationRepository.countAssignedReferee(raceId, currentUser.getUserId()) == 0) {
            throw new IllegalArgumentException("Referee has not been assigned to this race.");
        }
        Integer refereeId = violationRepository.findRefereeIdByUserId(currentUser.getUserId());
        if (refereeId == null) {
            throw new IllegalArgumentException("Current user does not have a Referee profile.");
        }
        return refereeId;
    }

    private void validateRequest(ViolationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Violation data is invalid.");
        }
        if (request.getEntryId() == null) {
            throw new IllegalArgumentException("entryId is required.");
        }
    }

    private String normalizeViolationType(String violationType) {
        if (violationType == null || violationType.isBlank()) {
            throw new IllegalArgumentException("Violation type is required.");
        }

        String trimmed = violationType.trim();
        String legacyType = switch (trimmed) {
            case "XuatPhatSai" -> "FalseStart";
            case "LanLane" -> "LaneViolation";
            case "CanDuong" -> "Obstruction";
            case "ViPhamNang" -> "SeriousViolation";
            default -> trimmed;
        };
        if (RULE_BY_VIOLATION.containsKey(legacyType)) {
            return legacyType;
        }

        return RULE_BY_VIOLATION.entrySet()
                .stream()
                .filter(entry -> entry.getValue().label().equalsIgnoreCase(legacyType))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Violation type is invalid."));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ViolationResponse toResponse(Violation violation) {
        return new ViolationResponse(
                violation.getViolationId(),
                violation.getRaceId(),
                violation.getEntryId(),
                violation.getRefereeId(),
                violation.getViolationType(),
                violation.getPenaltySeconds(),
                violation.getIsDq(),
                violation.getEvidenceImageUrl(),
                violation.getDescription(),
                violation.getRecordedAt()
        );
    }

    private record ViolationRule(String label, BigDecimal penaltySeconds, boolean isDq) {
    }
}
