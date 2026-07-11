package com.horseracing.service;

import com.horseracing.dto.RaceMinuteRequest;
import com.horseracing.dto.RaceMinuteResponse;
import com.horseracing.entity.RaceMinute;
import com.horseracing.entity.User;
import com.horseracing.repository.RaceMinuteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RaceMinuteService {

    private final RaceMinuteRepository raceMinuteRepository;

    public RaceMinuteService(RaceMinuteRepository raceMinuteRepository) {
        this.raceMinuteRepository = raceMinuteRepository;
    }

    public RaceMinuteResponse getMinutesByRace(Integer raceId) {
        ensureRaceExists(raceId);
        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race nay chua co bien ban"));

        return toResponse(minute);
    }

    public RaceMinuteResponse createMinutes(Integer raceId, RaceMinuteRequest request, User currentUser) {
        ensureRaceExists(raceId);
        Integer refereeId = ensureAssignedReferee(raceId, currentUser);
        validateRequest(request);

        if (raceMinuteRepository.existsByRaceId(raceId)) {
            throw new IllegalArgumentException("Race nay da co bien ban");
        }

        LocalDateTime now = LocalDateTime.now();

        RaceMinute minute = new RaceMinute();
        minute.setRaceId(raceId);
        minute.setRefereeId(refereeId);
        minute.setContent(request.getContent());
        minute.setPreRaceChecks(request.getPreRaceChecks());
        minute.setPostRaceNotes(resolvePostRaceNotes(request));
        minute.setMinutesFileUrl(request.getMinutesFileUrl());
        minute.setSentToOwners(false);
        minute.setCreatedAt(now);
        minute.setUpdatedAt(now);

        return toResponse(raceMinuteRepository.save(minute));
    }

    public RaceMinuteResponse updateMinutes(Integer raceId, RaceMinuteRequest request, User currentUser) {
        ensureRaceExists(raceId);
        Integer refereeId = ensureAssignedReferee(raceId, currentUser);
        validateRequest(request);

        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race nay chua co bien ban"));

        minute.setRefereeId(refereeId);
        minute.setContent(request.getContent());
        minute.setPreRaceChecks(request.getPreRaceChecks());
        minute.setPostRaceNotes(resolvePostRaceNotes(request));
        minute.setMinutesFileUrl(request.getMinutesFileUrl());
        minute.setUpdatedAt(LocalDateTime.now());

        return toResponse(raceMinuteRepository.save(minute));
    }

    @Transactional
    public RaceMinuteResponse sendMinutesToOwners(Integer raceId, User currentUser) {
        ensureRaceExists(raceId);
        ensureAssignedReferee(raceId, currentUser);

        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race nay chua co bien ban"));
        if (minute.getMinutesFileUrl() == null || minute.getMinutesFileUrl().isBlank()) {
            throw new IllegalArgumentException("Can co minutesFileUrl truoc khi gui bien ban cho owner");
        }

        raceMinuteRepository.sendMinutesToOwners(raceId);
        return toResponse(raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race nay chua co bien ban")));
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || raceMinuteRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Khong tim thay race");
        }
    }

    private Integer ensureAssignedReferee(Integer raceId, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null
                || !"Referee".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            throw new IllegalArgumentException("Chi Referee moi duoc lap bien ban");
        }
        if (raceMinuteRepository.countAssignedReferee(raceId, currentUser.getUserId()) == 0) {
            throw new IllegalArgumentException("Referee chua duoc phan cong cho race nay");
        }
        Integer refereeId = raceMinuteRepository.findRefereeIdByUserId(currentUser.getUserId());
        if (refereeId == null) {
            throw new IllegalArgumentException("User hien tai chua co ho so Referee");
        }
        return refereeId;
    }

    private void validateRequest(RaceMinuteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu bien ban khong hop le");
        }
        if (request.getMinutesFileUrl() == null || request.getMinutesFileUrl().isBlank()) {
            throw new IllegalArgumentException("minutesFileUrl khong duoc de trong");
        }
    }

    private String resolvePostRaceNotes(RaceMinuteRequest request) {
        if (request.getPostRaceNotes() != null) {
            return request.getPostRaceNotes();
        }

        return request.getNote();
    }

    private RaceMinuteResponse toResponse(RaceMinute minute) {
        return new RaceMinuteResponse(
                minute.getMinuteId(),
                minute.getRaceId(),
                minute.getRefereeId(),
                minute.getContent(),
                minute.getPreRaceChecks(),
                minute.getPostRaceNotes(),
                minute.getMinutesFileUrl(),
                minute.getSentToOwners(),
                minute.getSentAt(),
                minute.getCreatedAt(),
                minute.getUpdatedAt()
        );
    }
}
