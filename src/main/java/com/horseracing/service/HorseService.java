package com.horseracing.service;

import com.horseracing.dto.HorseHealthRecordRequest;
import com.horseracing.dto.HorseHealthRecordResponse;
import com.horseracing.dto.HorseRequest;
import com.horseracing.dto.HorseResponse;
import com.horseracing.dto.HorseStatusRequest;
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
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
public class HorseService {

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
        ensureCanAccessHorse(user, horse);

        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status khong duoc de trong");
        }

        horse.setIsActive(parseActiveStatus(request.getStatus()));
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
        ensureCanAccessHorse(user, horse);

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

        String latestHealth = firstNonBlank(request.getHealthStatus(), request.getDiagnosis());
        if (latestHealth != null) {
            horse.setHealthStatus(latestHealth);
            horseRepository.save(horse);
        }

        return toHealthResponse(healthRecordRepository.save(record));
    }

    private void applyHorseFields(Horse horse, HorseRequest request) {
        horse.setHorseName(request.getHorseName().trim());
        horse.setBreed(trimToNull(request.getBreed()));
        horse.setBirthYear(resolveBirthYear(request));
        horse.setColor(trimToNull(request.getColor()));
        horse.setGender(trimToNull(request.getGender()));
        horse.setWeightKg(resolveWeight(request));
        horse.setRegisterCode(trimToNull(request.getRegisterCode()));
        horse.setHealthStatus(trimToNull(request.getHealthStatus()));
        horse.setPhotoUrl(trimToNull(request.getPhotoUrl()));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            horse.setIsActive(parseActiveStatus(request.getStatus()));
        }
    }

    private void validateHorseRequest(HorseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu horse khong hop le");
        }
        if (request.getHorseName() == null || request.getHorseName().isBlank()) {
            throw new IllegalArgumentException("horseName khong duoc de trong");
        }

        BigDecimal weight = resolveWeight(request);
        if (weight != null && weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("weight phai lon hon 0");
        }

        Integer birthYear = resolveBirthYear(request);
        int currentYear = Year.now().getValue();
        if (birthYear != null && (birthYear < 1980 || birthYear > currentYear)) {
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
        if (currentUserService.isAdmin(user)) {
            return;
        }

        HorseOwner owner = getOwnerByUserId(user.getUserId());
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("Ban khong co quyen truy cap horse nay");
        }
    }

    private Integer resolveBirthYear(HorseRequest request) {
        if (request.getBirthYear() != null) {
            return request.getBirthYear();
        }
        if (request.getAge() != null) {
            if (request.getAge() <= 0) {
                throw new IllegalArgumentException("age phai lon hon 0");
            }
            return Year.now().getValue() - request.getAge();
        }
        return null;
    }

    private BigDecimal resolveWeight(HorseRequest request) {
        return request.getWeightKg() != null ? request.getWeightKg() : request.getWeight();
    }

    private Boolean parseActiveStatus(String status) {
        if ("Active".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status)) {
            return true;
        }
        if ("Inactive".equalsIgnoreCase(status) || "false".equalsIgnoreCase(status)) {
            return false;
        }
        throw new IllegalArgumentException("status chi chap nhan Active hoac Inactive");
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
                active ? "Active" : "Inactive",
                active,
                horse.getCreatedAt(),
                horse.getUpdatedAt()
        );
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
