package com.horseracing.service;

import com.horseracing.dto.ViolationRequest;
import com.horseracing.dto.ViolationResponse;
import com.horseracing.entity.Violation;
import com.horseracing.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViolationService {

    private static final int DEFAULT_REFEREE_ID = 1;

    private final ViolationRepository violationRepository;

    public ViolationService(ViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }

    public List<ViolationResponse> getViolationsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        return violationRepository.findByRaceId(raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ViolationResponse createViolation(Integer raceId, ViolationRequest request) {
        ensureRaceExists(raceId);
        ensureEntryBelongsToRace(raceId, request.getEntryId());
        validateViolationType(request.getViolationType());

        Integer refereeId = resolveRefereeId(request.getRefereeId());

        Violation violation = new Violation();
        violation.setRaceId(raceId);
        violation.setEntryId(request.getEntryId());
        violation.setRefereeId(refereeId);
        violation.setViolationType(request.getViolationType());
        violation.setDescription(request.getDescription());
        violation.setPenalty(request.getPenalty());
        violation.setRecordedAt(LocalDateTime.now());

        return toResponse(violationRepository.save(violation));
    }

    public ViolationResponse updateViolation(Integer violationId, ViolationRequest request) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vi phạm"));

        if (request.getEntryId() != null) {
            ensureEntryBelongsToRace(violation.getRaceId(), request.getEntryId());
            violation.setEntryId(request.getEntryId());
        }

        if (request.getRefereeId() != null) {
            violation.setRefereeId(resolveRefereeId(request.getRefereeId()));
        }

        if (request.getViolationType() != null) {
            validateViolationType(request.getViolationType());
            violation.setViolationType(request.getViolationType());
        }

        violation.setDescription(request.getDescription());
        violation.setPenalty(request.getPenalty());

        return toResponse(violationRepository.save(violation));
    }

    public void deleteViolation(Integer violationId) {
        if (!violationRepository.existsById(violationId)) {
            throw new IllegalArgumentException("Không tìm thấy vi phạm");
        }

        violationRepository.deleteById(violationId);
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || violationRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Không tìm thấy race");
        }
    }

    private void ensureEntryBelongsToRace(Integer raceId, Integer entryId) {
        if (entryId == null || violationRepository.countEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("Entry không thuộc race này");
        }
    }

    private Integer resolveRefereeId(Integer refereeId) {
        Integer resolvedRefereeId = refereeId == null ? DEFAULT_REFEREE_ID : refereeId;
        if (violationRepository.countRefereeById(resolvedRefereeId) == 0) {
            throw new IllegalArgumentException("Không tìm thấy referee");
        }

        return resolvedRefereeId;
    }

    private void validateViolationType(String violationType) {
        if (violationType == null || violationType.isBlank()) {
            throw new IllegalArgumentException("violationType không được để trống");
        }
    }

    private ViolationResponse toResponse(Violation violation) {
        return new ViolationResponse(
                violation.getViolationId(),
                violation.getRaceId(),
                violation.getEntryId(),
                violation.getRefereeId(),
                violation.getViolationType(),
                violation.getDescription(),
                violation.getPenalty(),
                violation.getRecordedAt()
        );
    }
}
