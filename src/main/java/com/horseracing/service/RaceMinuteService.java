package com.horseracing.service;

import com.horseracing.dto.RaceMinuteRequest;
import com.horseracing.dto.RaceMinuteResponse;
import com.horseracing.entity.RaceMinute;
import com.horseracing.repository.RaceMinuteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RaceMinuteService {

    private static final int DEFAULT_REFEREE_ID = 1;

    private final RaceMinuteRepository raceMinuteRepository;

    public RaceMinuteService(RaceMinuteRepository raceMinuteRepository) {
        this.raceMinuteRepository = raceMinuteRepository;
    }

    public RaceMinuteResponse getMinutesByRace(Integer raceId) {
        ensureRaceExists(raceId);
        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race này chưa có biên bản"));

        return toResponse(minute);
    }

    public RaceMinuteResponse createMinutes(Integer raceId, RaceMinuteRequest request) {
        ensureRaceExists(raceId);

        if (raceMinuteRepository.existsByRaceId(raceId)) {
            throw new IllegalArgumentException("Race này đã có biên bản");
        }

        Integer refereeId = resolveRefereeId(request.getRefereeId());
        LocalDateTime now = LocalDateTime.now();

        RaceMinute minute = new RaceMinute();
        minute.setRaceId(raceId);
        minute.setRefereeId(refereeId);
        minute.setContent(request.getContent());
        minute.setPreRaceChecks(request.getPreRaceChecks());
        minute.setPostRaceNotes(resolvePostRaceNotes(request));
        minute.setCreatedAt(now);
        minute.setUpdatedAt(now);

        return toResponse(raceMinuteRepository.save(minute));
    }

    public RaceMinuteResponse updateMinutes(Integer raceId, RaceMinuteRequest request) {
        ensureRaceExists(raceId);

        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race này chưa có biên bản"));

        if (request.getRefereeId() != null) {
            minute.setRefereeId(resolveRefereeId(request.getRefereeId()));
        }

        minute.setContent(request.getContent());
        minute.setPreRaceChecks(request.getPreRaceChecks());
        minute.setPostRaceNotes(resolvePostRaceNotes(request));
        minute.setUpdatedAt(LocalDateTime.now());

        return toResponse(raceMinuteRepository.save(minute));
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || raceMinuteRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Không tìm thấy race");
        }
    }

    private Integer resolveRefereeId(Integer refereeId) {
        Integer resolvedRefereeId = refereeId == null ? DEFAULT_REFEREE_ID : refereeId;
        if (raceMinuteRepository.countRefereeById(resolvedRefereeId) == 0) {
            throw new IllegalArgumentException("Không tìm thấy referee");
        }

        return resolvedRefereeId;
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
                minute.getCreatedAt(),
                minute.getUpdatedAt()
        );
    }
}
