package com.horseracing.service;

import com.horseracing.dto.ViolationOptionResponse;
import com.horseracing.dto.ViolationRequest;
import com.horseracing.dto.ViolationResponse;
import com.horseracing.entity.Violation;
import com.horseracing.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ViolationService {

    private static final int DEFAULT_REFEREE_ID = 1;
    private static final Map<String, String> PENALTY_BY_VIOLATION = Map.ofEntries(
            Map.entry("XuatPhatSom", "Cảnh cáo và cộng 5 giây vào thành tích"),
            Map.entry("ChaySaiLan", "Phạt 10 giây"),
            Map.entry("CanTroDoiThu", "Hủy kết quả vòng đua"),
            Map.entry("VuotRaKhoiDuongDua", "Hủy kết quả vòng đua"),
            Map.entry("DungRoiTiepTucSaiQuyDinh", "Phạt 15 giây"),
            Map.entry("SuDungThietBiCam", "Loại khỏi cuộc đua"),
            Map.entry("BaoHanhNguocDaiNgua", "Loại khỏi giải đấu"),
            Map.entry("KhongTuanThuTrongTai", "Cảnh cáo hoặc loại khỏi cuộc đua")
    );

    private static final Map<String, String> LABEL_BY_VIOLATION = Map.ofEntries(
            Map.entry("XuatPhatSom", "Xuất phát sớm"),
            Map.entry("ChaySaiLan", "Chạy sai làn"),
            Map.entry("CanTroDoiThu", "Cản trở đối thủ"),
            Map.entry("VuotRaKhoiDuongDua", "Vượt ra khỏi đường đua"),
            Map.entry("DungRoiTiepTucSaiQuyDinh", "Dừng rồi tiếp tục sai quy định"),
            Map.entry("SuDungThietBiCam", "Sử dụng thiết bị cấm"),
            Map.entry("BaoHanhNguocDaiNgua", "Bạo hành hoặc ngược đãi ngựa"),
            Map.entry("KhongTuanThuTrongTai", "Không tuân thủ trọng tài")
    );

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

    public List<ViolationOptionResponse> getViolationOptions() {
        return PENALTY_BY_VIOLATION.entrySet()
                .stream()
                .map(entry -> new ViolationOptionResponse(
                        entry.getKey(),
                        LABEL_BY_VIOLATION.get(entry.getKey()),
                        entry.getValue()
                ))
                .toList();
    }

    public ViolationResponse createViolation(Integer raceId, ViolationRequest request) {
        ensureRaceExists(raceId);
        ensureEntryBelongsToRace(raceId, request.getEntryId());
        String violationType = normalizeViolationType(request.getViolationType());

        Integer refereeId = resolveRefereeId(request.getRefereeId());

        Violation violation = new Violation();
        violation.setRaceId(raceId);
        violation.setEntryId(request.getEntryId());
        violation.setRefereeId(refereeId);
        violation.setViolationType(violationType);
        violation.setDescription(request.getDescription());
        violation.setPenalty(resolvePenalty(violationType, request.getPenalty()));
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
            violation.setViolationType(normalizeViolationType(request.getViolationType()));
        }

        violation.setDescription(request.getDescription());
        violation.setPenalty(resolvePenalty(violation.getViolationType(), request.getPenalty()));

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
            throw new IllegalArgumentException("Không tìm thấy vòng đua");
        }
    }

    private void ensureEntryBelongsToRace(Integer raceId, Integer entryId) {
        if (entryId == null || violationRepository.countEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("Entry không thuộc vòng đua này");
        }
    }

    private Integer resolveRefereeId(Integer refereeId) {
        Integer resolvedRefereeId = refereeId == null ? DEFAULT_REFEREE_ID : refereeId;
        if (violationRepository.countRefereeById(resolvedRefereeId) == 0) {
            throw new IllegalArgumentException("Không tìm thấy trọng tài");
        }

        return resolvedRefereeId;
    }

    private String normalizeViolationType(String violationType) {
        if (violationType == null || violationType.isBlank()) {
            throw new IllegalArgumentException("Loại vi phạm không được để trống");
        }

        String trimmed = violationType.trim();
        if (PENALTY_BY_VIOLATION.containsKey(trimmed)) {
            return trimmed;
        }

        return LABEL_BY_VIOLATION.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(trimmed))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Loại vi phạm không hợp lệ"));
    }

    private String resolvePenalty(String violationType, String requestedPenalty) {
        if (requestedPenalty != null && !requestedPenalty.isBlank()) {
            return requestedPenalty.trim();
        }
        return PENALTY_BY_VIOLATION.get(violationType);
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
