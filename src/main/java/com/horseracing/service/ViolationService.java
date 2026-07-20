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
import java.util.List;
import java.util.Map;

@Service
public class ViolationService {

    private static final Map<String, ViolationRule> RULE_BY_VIOLATION = Map.ofEntries(
            Map.entry("XuatPhatSai", new ViolationRule("Xuat phat sai", BigDecimal.valueOf(3), false)),
            Map.entry("LanLane", new ViolationRule("Lan lane", BigDecimal.valueOf(5), false)),
            Map.entry("CanDuong", new ViolationRule("Can duong", BigDecimal.valueOf(10), false)),
            Map.entry("ViPhamNang", new ViolationRule("Vi pham nang", BigDecimal.ZERO, true))
    );

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
        violation.setEvidenceImageUrl(request.getEvidenceImageUrl());
        violation.setDescription(request.getDescription());
        violation.setRecordedAt(LocalDateTime.now());

        Violation saved = violationRepository.save(violation);
        raceRankingService.recalculateRace(raceId);
        return toResponse(saved);
    }

    public ViolationResponse updateViolation(Integer violationId, ViolationRequest request, User currentUser) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu vi pham khong hop le");
        }
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay vi pham"));
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

        violation.setEvidenceImageUrl(request.getEvidenceImageUrl());
        violation.setDescription(request.getDescription());

        Violation saved = violationRepository.save(violation);
        raceRankingService.recalculateRace(saved.getRaceId());
        return toResponse(saved);
    }

    public void deleteViolation(Integer violationId, User currentUser) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay vi pham"));
        ensureAssignedReferee(violation.getRaceId(), currentUser);
        Integer raceId = violation.getRaceId();
        violationRepository.deleteById(violationId);
        raceRankingService.recalculateRace(raceId);
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || violationRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Khong tim thay race");
        }
    }

    private void ensureEntryBelongsToRace(Integer raceId, Integer entryId) {
        if (entryId == null || violationRepository.countEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("Entry khong thuoc race nay");
        }
    }

    private Integer ensureAssignedReferee(Integer raceId, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null
                || !"Referee".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            throw new IllegalArgumentException("Chi Referee moi duoc ghi vi pham");
        }
        if (violationRepository.countAssignedReferee(raceId, currentUser.getUserId()) == 0) {
            throw new IllegalArgumentException("Referee chua duoc phan cong cho race nay");
        }
        Integer refereeId = violationRepository.findRefereeIdByUserId(currentUser.getUserId());
        if (refereeId == null) {
            throw new IllegalArgumentException("User hien tai chua co ho so Referee");
        }
        return refereeId;
    }

    private void validateRequest(ViolationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu vi pham khong hop le");
        }
        if (request.getEntryId() == null) {
            throw new IllegalArgumentException("entryId khong duoc de trong");
        }
    }

    private String normalizeViolationType(String violationType) {
        if (violationType == null || violationType.isBlank()) {
            throw new IllegalArgumentException("Loai vi pham khong duoc de trong");
        }

        String trimmed = violationType.trim();
        if (RULE_BY_VIOLATION.containsKey(trimmed)) {
            return trimmed;
        }

        return RULE_BY_VIOLATION.entrySet()
                .stream()
                .filter(entry -> entry.getValue().label().equalsIgnoreCase(trimmed))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Loai vi pham khong hop le"));
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
