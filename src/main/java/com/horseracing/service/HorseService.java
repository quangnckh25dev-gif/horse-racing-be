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
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HorseService {

    private static final String HEALTH_ACTIVE = "Active";
    private static final String HEALTH_INJURED = "Injured";
    private static final String HEALTH_INACTIVE = "Inactive";

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

    public List<HorseResponse> getHorses(String keyword, String status, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        List<Horse> horses;
        if (currentUserService.isAdmin(user)) {
            horses = horseRepository.findAll();
        } else if (isOrganizer(user)) {
            horses = horseRepository.findAll();
        } else {
            HorseOwner owner = getOwnerByUserId(user.getUserId());
            horses = horseRepository.findByOwnerId(owner.getOwnerId());
        }

        return horses.stream()
                .filter(horse -> matchesKeyword(horse, keyword))
                .filter(horse -> matchesStatus(horse, status))
                .map(this::toHorseResponse)
                .toList();
    }

    public HorseOptionsResponse getHorseOptions() {
        return new HorseOptionsResponse(
                List.of(
                        new OptionResponse("Active", "Active"),
                        new OptionResponse("Injured", "Injured"),
                        new OptionResponse("Inactive", "Inactive")
                ),
                List.of(
                        new OptionResponse("Black", "Black"),
                        new OptionResponse("White", "White"),
                        new OptionResponse("Brown", "Brown"),
                        new OptionResponse("Dark Brown", "Dark Brown"),
                        new OptionResponse("Chestnut", "Chestnut"),
                        new OptionResponse("Gold", "Gold"),
                        new OptionResponse("Gray", "Gray"),
                        new OptionResponse("Dapple Gray", "Dapple Gray"),
                        new OptionResponse("Bay", "Bay"),
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
        ensureUniqueHorseName(owner.getOwnerId(), request.getHorseName(), null);

        Horse horse = new Horse();
        horse.setOwnerId(owner.getOwnerId());
        applyHorseFields(horse, request, user);
        if (horse.getRegisterCode() == null) {
            horse.setRegisterCode(generateRegisterCode());
        }

        return toHorseResponse(horseRepository.save(horse));
    }

    public HorseResponse updateHorse(Integer horseId, HorseRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureOwnerOwnsHorse(user, horse);
        validateHorseRequest(request);
        ensureUniqueHorseName(horse.getOwnerId(), request.getHorseName(), horse.getHorseId());
        applyHorseFields(horse, request, user);

        return toHorseResponse(horseRepository.save(horse));
    }

    public HorseResponse updateHorseStatus(Integer horseId, HorseStatusRequest request, HttpServletRequest httpRequest) {
        // Horse owner or organizer updates horse status.
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanUpdateHorseStatus(user, horse);

        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required.");
        }

        applyHorseStatus(horse, request.getStatus(), user);
        return toHorseResponse(horseRepository.save(horse));
    }

    public List<HorseHealthRecordResponse> getHealthHistory(Integer horseId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanAccessHealthHistory(user, horse);

        return healthRecordRepository.findByHorseIdOrderByCheckDateDesc(horseId)
                .stream()
                .map(this::toHealthResponse)
                .toList();
    }

    public HorseHealthRecordResponse addHealthRecord(Integer horseId, HorseHealthRecordRequest request, HttpServletRequest httpRequest) {
        // Organizer adds a horse health record.
        User user = currentUserService.getCurrentUser(httpRequest);
        Horse horse = getHorseEntity(horseId);
        ensureCanManageHorseHealth(user);

        if (request == null) {
            throw new IllegalArgumentException("Health record data is invalid.");
        }
        if (request.getCheckDate() == null) {
            throw new IllegalArgumentException("checkDate is required.");
        }

        HorseHealthRecord record = new HorseHealthRecord();
        record.setHorseId(horseId);
        record.setCheckDate(request.getCheckDate());
        record.setVetName(request.getVetName());
        record.setDiagnosis(firstNonBlank(request.getDiagnosis(), request.getHealthStatus()));
        record.setNotes(firstNonBlank(request.getNotes(), request.getNote()));

        String latestHealth = trimToNull(request.getHealthStatus());
        if (latestHealth != null) {
            applyHorseStatus(horse, latestHealth, user);
            horseRepository.save(horse);
        }

        return toHealthResponse(healthRecordRepository.save(record));
    }

    private void applyHorseFields(Horse horse, HorseRequest request, User updatedBy) {
        horse.setHorseName(request.getHorseName().trim());
        horse.setBreed(trimToNull(request.getBreed()));
        horse.setBirthYear(resolveBirthYear(request));
        horse.setColor(trimToNull(request.getColor()));
        horse.setGender(trimToNull(request.getGender()));
        horse.setWeightKg(resolveWeight(request));
        if (horse.getRegisterCode() == null) {
            horse.setRegisterCode(generateRegisterCode());
        }
        String requestedStatus = firstNonBlank(request.getStatus(), request.getHealthStatus());
        if (requestedStatus != null) {
            applyHorseStatus(horse, requestedStatus, updatedBy);
        } else if (horse.getHealthStatus() == null || horse.getHealthStatus().isBlank()) {
            horse.setHealthStatus(HEALTH_ACTIVE);
        }
        if (horse.getIsActive() == null) {
            horse.setIsActive(true);
        }
        horse.setPhotoUrl(trimToNull(request.getPhotoUrl()));
    }

    private void validateHorseRequest(HorseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Horse data is invalid.");
        }
        if (request.getHorseName() == null || request.getHorseName().isBlank()) {
            throw new IllegalArgumentException("horseName is required.");
        }

        BigDecimal weight = resolveWeight(request);
        if (weight == null) {
            throw new IllegalArgumentException("weightKg is required.");
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("weightKg must be greater than 0.");
        }

        Integer birthYear = resolveBirthYear(request);
        if (birthYear == null) {
            throw new IllegalArgumentException("birthYear is required.");
        }
        int currentYear = Year.now().getValue();
        if (birthYear < 1980 || birthYear > currentYear) {
            throw new IllegalArgumentException("birthYear is invalid.");
        }
    }

    private void ensureUniqueHorseName(Integer ownerId, String horseName, Integer ignoredHorseId) {
        if (horseRepository.existsDuplicateNameForOwner(ownerId, horseName.trim(), ignoredHorseId)) {
            throw new IllegalArgumentException("Horse name already exists for this owner.");
        }
    }

    private Horse getHorseEntity(Integer horseId) {
        if (horseId == null) {
            throw new IllegalArgumentException("horseId is required.");
        }
        return horseRepository.findById(horseId)
                .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
    }

    private HorseOwner getOwnerByUserId(Integer userId) {
        return horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a HorseOwner profile."));
    }

    private void ensureCanAccessHorse(User user, Horse horse) {
        if (currentUserService.isAdmin(user) || isOrganizer(user)) {
            return;
        }
        ensureOwnerOwnsHorse(user, horse);
    }

    private void ensureOwnerOwnsHorse(User user, Horse horse) {
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("You do not have permission to access this horse.");
        }
    }

    private BigDecimal resolveWeight(HorseRequest request) {
        if (request.getWeightKg() != null) {
            return request.getWeightKg();
        }
        return request.getWeight();
    }

    private Integer resolveBirthYear(HorseRequest request) {
        if (request.getBirthYear() != null) {
            return request.getBirthYear();
        }
        if (request.getAge() == null) {
            return null;
        }
        if (request.getAge() <= 0) {
            throw new IllegalArgumentException("age must be greater than 0.");
        }
        return Year.now().getValue() - request.getAge();
    }

    private void applyHorseStatus(Horse horse, String status, User updatedBy) {
        String normalized = normalizeStatus(status);
        if ("ACTIVE".equals(normalized)) {
            horse.setHealthStatus(HEALTH_ACTIVE);
            horse.setIsActive(true);
        } else if ("INJURED".equals(normalized)) {
            horse.setHealthStatus(HEALTH_INJURED);
            horse.setIsActive(true);
        } else {
            horse.setHealthStatus(HEALTH_INACTIVE);
            horse.setIsActive(false);
        }
        if (updatedBy != null) {
            horse.setHealthUpdatedBy(updatedBy.getUserId());
        }
        horse.setHealthUpdatedAt(LocalDateTime.now());
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized.equals("active") || normalized.equals("hoat dong")
                || normalized.equals("true")) {
            return "ACTIVE";
        }
        if (normalized.equals("injured") || normalized.equals("bi thuong")) {
            return "INJURED";
        }
        if (normalized.equals("inactive") || normalized.equals("unactive")
                || normalized.equals("khong hoat dong") || normalized.equals("false")) {
            return "INACTIVE";
        }
        throw new IllegalArgumentException("Horse status only accepts: Active, Injured, Inactive.");
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeyword(Horse horse, String keyword) {
        String normalizedKeyword = normalizeText(keyword);
        if (normalizedKeyword.isBlank()) {
            return true;
        }
        return normalizeText(horse.getHorseName()).contains(normalizedKeyword)
                || normalizeText(horse.getBreed()).contains(normalizedKeyword)
                || normalizeText(horse.getRegisterCode()).contains(normalizedKeyword)
                || normalizeText(horse.getColor()).contains(normalizedKeyword)
                || normalizeText(horse.getGender()).contains(normalizedKeyword);
    }

    private boolean matchesStatus(Horse horse, String status) {
        String normalizedStatus = normalizeText(status);
        if (normalizedStatus.isBlank()) {
            return true;
        }
        return normalizeText(resolveStatusCode(horse)).equals(normalizedStatus)
                || normalizeText(horse.getHealthStatus()).equals(normalizedStatus);
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
                resolveStatusCode(horse),
                active,
                horse.getCreatedAt(),
                horse.getUpdatedAt()
        );
    }

    private String resolveStatusCode(Horse horse) {
        // Health text can be free-form, so status display falls back to isActive when it cannot be normalized.
        String normalized = safeNormalizeStatus(horse.getHealthStatus());
        if ("INJURED".equals(normalized)) {
            return "Injured";
        }
        if ("INACTIVE".equals(normalized)) {
            return "Inactive";
        }
        return Boolean.TRUE.equals(horse.getIsActive()) ? "Active" : "Inactive";
    }

    // Read-only normalization used for display.
    private String safeNormalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return normalizeStatus(status);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void ensureCanUpdateHorseStatus(User user, Horse horse) {
        if (isOrganizer(user)) {
            return;
        }
        ensureOwnerOwnsHorse(user, horse);
    }

    private void ensureCanManageHorseHealth(User user) {
        if (isOrganizer(user)) {
            return;
        }
        throw new IllegalArgumentException("Only organizers can update horse health.");
    }

    private void ensureCanAccessHealthHistory(User user, Horse horse) {
        if (isOrganizer(user)) {
            return;
        }
        ensureOwnerOwnsHorse(user, horse);
    }

    private boolean isOrganizer(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }
        String roleName = user.getRole().getRoleName();
        return "Organizer".equalsIgnoreCase(roleName)
                && Boolean.TRUE.equals(user.getIsActive())
                && Boolean.TRUE.equals(user.getIsApproved());
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
