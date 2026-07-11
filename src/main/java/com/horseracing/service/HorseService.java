package com.horseracing.service;

import com.horseracing.dto.HorseHealthRecordRequest;
import com.horseracing.dto.HorseHealthRecordResponse;
import com.horseracing.dto.HorseOptionsResponse;
import com.horseracing.dto.HorseRequest;
import com.horseracing.dto.HorseResponse;
import com.horseracing.dto.HorseStatusRequest;
import com.horseracing.dto.OptionResponse;
import com.horseracing.entity.Horse;
import com.horseracing.entity.HorseHealthRecord;
import com.horseracing.entity.HorseOwner;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseHealthRecordRepository;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.HorseRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HorseService {

    private static final String HEALTH_ACTIVE = "Hoạt động";
    private static final String HEALTH_INJURED = "Bị thương";
    private static final String HEALTH_INACTIVE = "Không hoạt động";

    private final HorseRepository horseRepository;
    private final HorseOwnerRepository horseOwnerRepository;
    private final HorseHealthRecordRepository healthRecordRepository;
    private final CurrentUserService currentUserService;

    public HorseService(HorseRepository horseRepository,
                        HorseOwnerRepository horseOwnerRepository,
                        HorseHealthRecordRepository healthRecordRepository,
                        CurrentUserService currentUserService) {
        this.horseRepository = horseRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.currentUserService = currentUserService;
    }

    public List<HorseResponse> getHorses(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        if (currentUserService.isAdmin(user)) {
            return horseRepository.findAll().stream().map(this::toHorseResponse).toList();
        }

        HorseOwner owner = getOwnerByUserId(user.getUserId());
        return horseRepository.findByOwnerId(owner.getOwnerId())
                .stream()
                .map(this::toHorseResponse)
                .toList();
    }

    public HorseOptionsResponse getHorseOptions() {
        return new HorseOptionsResponse(
                List.of(
                        new OptionResponse("Hoạt động", "Hoạt động"),
                        new OptionResponse("Bị thương", "Bị thương"),
                        new OptionResponse("Không hoạt động", "Không hoạt động")
                ),
                List.of(
                        new OptionResponse("Đen", "Đen"),
                        new OptionResponse("Trắng", "Trắng"),
                        new OptionResponse("Nâu", "Nâu"),
                        new OptionResponse("Nâu đậm", "Nâu đậm"),
                        new OptionResponse("Nâu đỏ", "Nâu đỏ"),
                        new OptionResponse("Vàng", "Vàng"),
                        new OptionResponse("Xám", "Xám"),
                        new OptionResponse("Xám đốm", "Xám đốm"),
                        new OptionResponse("Hạt dẻ", "Hạt dẻ"),
                        new OptionResponse("Palomino", "Palomino"),
                        new OptionResponse("Pinto", "Pinto"),
                        new OptionResponse("Appaloosa", "Appaloosa")
                ),
                List.of(
                        new OptionResponse("Thoroughbred", "Thoroughbred"),
                        new OptionResponse("Arabian", "Arabian"),
                        new OptionResponse("Quarter Horse", "Quarter Horse"),
                        new OptionResponse("Standardbred", "Standardbred"),
                        new OptionResponse("Andalusian", "Andalusian"),
                        new OptionResponse("Appaloosa", "Appaloosa"),
                        new OptionResponse("Akhal-Teke", "Akhal-Teke"),
                        new OptionResponse("Friesian", "Friesian"),
                        new OptionResponse("Mustang", "Mustang"),
                        new OptionResponse("Morgan", "Morgan"),
                        new OptionResponse("Hanoverian", "Hanoverian"),
                        new OptionResponse("Warmblood", "Warmblood")
                )
        );
    }

    public HorseResponse getHorse(Integer horseId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanAccessHorse(user, horse);
        return toHorseResponse(horse);
    }

    public HorseResponse createHorse(HorseRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        validateHorseRequest(request);

        Horse horse = new Horse();
        horse.setOwnerId(owner.getOwnerId());
        applyHorseFields(horse, request);
        if (horse.getRegisterCode() == null) {
            horse.setRegisterCode(generateRegisterCode());
        }

        return toHorseResponse(horseRepository.save(horse));
    }

    public HorseResponse updateHorse(Integer horseId, HorseRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanAccessHorse(user, horse);
        validateHorseRequest(request);
        applyHorseFields(horse, request);

        return toHorseResponse(horseRepository.save(horse));
    }

    public HorseResponse updateHorseStatus(Integer horseId, HorseStatusRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanManageHorseHealth(user);

        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status khong duoc de trong");
        }

        applyHorseStatus(horse, request.getStatus());
        return toHorseResponse(horseRepository.save(horse));
    }

    public List<HorseHealthRecordResponse> getHealthHistory(Integer horseId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanAccessHorse(user, horse);

        return healthRecordRepository.findByHorseIdOrderByCheckDateDesc(horseId)
                .stream()
                .map(this::toHealthResponse)
                .toList();
    }

    public HorseHealthRecordResponse addHealthRecord(Integer horseId, HorseHealthRecordRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanManageHorseHealth(user);

        if (request == null) {
            throw new IllegalArgumentException("Du lieu health record khong hop le");
        }
        if (request.getCheckDate() == null) {
            throw new IllegalArgumentException("checkDate khong duoc de trong");
        }

        HorseHealthRecord record = new HorseHealthRecord();
        record.setHorseId(horseId);
        record.setCheckDate(request.getCheckDate());
        record.setVetName(request.getVetName());
        record.setDiagnosis(firstNonBlank(request.getDiagnosis(), request.getHealthStatus()));
        record.setNotes(firstNonBlank(request.getNotes(), request.getNote()));

        String latestHealth = trimToNull(request.getHealthStatus());
        if (latestHealth != null) {
            applyHorseStatus(horse, latestHealth);
            horseRepository.save(horse);
        }

        return toHealthResponse(healthRecordRepository.save(record));
    }

    private void applyHorseFields(Horse horse, HorseRequest request) {
        horse.setHorseName(request.getHorseName().trim());
        horse.setBreed(trimToNull(request.getBreed()));
        horse.setBirthYear(request.getBirthYear());
        horse.setColor(trimToNull(request.getColor()));
        horse.setGender(trimToNull(request.getGender()));
        horse.setWeightKg(resolveWeight(request));
        if (horse.getRegisterCode() == null) {
            horse.setRegisterCode(generateRegisterCode());
        }
        if (horse.getHealthStatus() == null || horse.getHealthStatus().isBlank()) {
            horse.setHealthStatus(HEALTH_ACTIVE);
        }
        if (horse.getIsActive() == null) {
            horse.setIsActive(true);
        }
        horse.setPhotoUrl(trimToNull(request.getPhotoUrl()));
    }

    private void validateHorseRequest(HorseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu horse khong hop le");
        }
        if (request.getHorseName() == null || request.getHorseName().isBlank()) {
            throw new IllegalArgumentException("horseName khong duoc de trong");
        }

        BigDecimal weight = request.getWeightKg();
        if (weight == null) {
            throw new IllegalArgumentException("weightKg khong duoc de trong");
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("weightKg phai lon hon 0");
        }

        Integer birthYear = request.getBirthYear();
        if (birthYear == null) {
            throw new IllegalArgumentException("birthYear khong duoc de trong");
        }
        int currentYear = Year.now().getValue();
        if (birthYear < 1980 || birthYear > currentYear) {
            throw new IllegalArgumentException("birthYear khong hop le");
        }
    }

    private Horse getHorseEntity(Integer horseId) {
        if (horseId == null) {
            throw new IllegalArgumentException("horseId khong duoc de trong");
        }
        return horseRepository.findById(horseId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay horse"));
    }

    private HorseOwner getOwnerByUserId(Integer userId) {
        return horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User hien tai chua co ho so HorseOwner"));
    }

    private void ensureCanAccessHorse(User user, Horse horse) {
        if (currentUserService.isAdmin(user) || isOrganizer(user)) {
            return;
        }

        HorseOwner owner = getOwnerByUserId(user.getUserId());
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("Ban khong co quyen truy cap horse nay");
        }
    }

    private BigDecimal resolveWeight(HorseRequest request) {
        return request.getWeightKg();
    }

    private void applyHorseStatus(Horse horse, String status) {
        String normalized = normalizeStatus(status);
        if ("ACTIVE".equals(normalized)) {
            horse.setIsActive(true);
            horse.setHealthStatus(HEALTH_ACTIVE);
            return;
        }
        if ("INJURED".equals(normalized)) {
            horse.setIsActive(false);
            horse.setHealthStatus(HEALTH_INJURED);
            return;
        }
        horse.setIsActive(false);
        horse.setHealthStatus(HEALTH_INACTIVE);
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("active") || normalized.equals("hoạt động") || normalized.equals("hoat dong")
                || normalized.equals("true")) {
            return "ACTIVE";
        }
        if (normalized.equals("injured") || normalized.equals("bị thương") || normalized.equals("bi thuong")) {
            return "INJURED";
        }
        if (normalized.equals("inactive") || normalized.equals("unactive") || normalized.equals("không hoạt động")
                || normalized.equals("khong hoat dong") || normalized.equals("false")) {
            return "INACTIVE";
        }
        throw new IllegalArgumentException("Trạng thái ngựa chỉ chấp nhận: Hoạt động, Bị thương, Không hoạt động");
    }

    private HorseResponse toHorseResponse(Horse horse) {
        Integer age = horse.getBirthYear() == null ? null : Year.now().getValue() - horse.getBirthYear();
        boolean active = Boolean.TRUE.equals(horse.getIsActive());
        return new HorseResponse(
                horse.getHorseId(),
                horse.getOwnerId(),
                horse.getHorseName(),
                horse.getBreed(),
                horse.getBirthYear(),
                age,
                horse.getColor(),
                horse.getGender(),
                horse.getWeightKg(),
                horse.getRegisterCode(),
                horse.getHealthStatus(),
                horse.getPhotoUrl(),
                resolveStatusLabel(horse),
                active,
                horse.getCreatedAt(),
                horse.getUpdatedAt()
        );
    }

    private String resolveStatusLabel(Horse horse) {
        String healthStatus = horse.getHealthStatus();
        if (healthStatus != null && "INJURED".equals(normalizeStatus(healthStatus))) {
            return HEALTH_INJURED;
        }
        return Boolean.TRUE.equals(horse.getIsActive()) ? HEALTH_ACTIVE : HEALTH_INACTIVE;
    }

    private void ensureCanManageHorseHealth(User user) {
        if (currentUserService.isAdmin(user) || isOrganizer(user)) {
            return;
        }
        throw new IllegalArgumentException("Chi Admin hoac Ban to chuc moi duoc cap nhat suc khoe ngua");
    }

    private boolean isOrganizer(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }
        String roleName = user.getRole().getRoleName();
        return "Organizer".equalsIgnoreCase(roleName)
                || "OrganizerHead".equalsIgnoreCase(roleName)
                || "OrganizerMember".equalsIgnoreCase(roleName);
    }

    private String generateRegisterCode() {
        String code;
        do {
            code = "HORSE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (horseRepository.existsByRegisterCode(code));
        return code;
    }

    private HorseHealthRecordResponse toHealthResponse(HorseHealthRecord record) {
        return new HorseHealthRecordResponse(
                record.getRecordId(),
                record.getHorseId(),
                record.getCheckDate(),
                record.getVetName(),
                record.getDiagnosis(),
                record.getNotes(),
                record.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
