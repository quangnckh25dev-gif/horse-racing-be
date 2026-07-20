package com.horseracing.service;

import com.horseracing.dto.RaceMinuteRequest;
import com.horseracing.dto.RaceMinuteResponse;
import com.horseracing.entity.Notification;
import com.horseracing.entity.RaceMinute;
import com.horseracing.entity.User;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceMinuteRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RaceMinuteService {

    private final RaceMinuteRepository raceMinuteRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public RaceMinuteService(RaceMinuteRepository raceMinuteRepository,
                             NotificationRepository notificationRepository,
                             UserRepository userRepository) {
        this.raceMinuteRepository = raceMinuteRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public RaceMinuteResponse getMinutesByRace(Integer raceId) {
        ensureRaceExists(raceId);
        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("This race does not have minutes yet."));

        return toResponse(minute);
    }

    public RaceMinuteResponse createMinutes(Integer raceId, RaceMinuteRequest request, User currentUser) {
        ensureRaceExists(raceId);
        Integer refereeId = ensureAssignedReferee(raceId, currentUser);
        validateRequest(request);

        if (raceMinuteRepository.existsByRaceId(raceId)) {
            throw new IllegalArgumentException("This race already has minutes.");
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
                .orElseThrow(() -> new IllegalArgumentException("This race does not have minutes yet."));

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
                .orElseThrow(() -> new IllegalArgumentException("This race does not have minutes yet."));
        if (minute.getMinutesFileUrl() == null || minute.getMinutesFileUrl().isBlank()) {
            throw new IllegalArgumentException("minutesFileUrl is required before sending minutes to the owner.");
        }

        raceMinuteRepository.sendMinutesToOwners(raceId);
        return toResponse(raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("This race does not have minutes yet.")));
    }

    @Transactional
    public RaceMinuteResponse handoffMinutesToOrganizer(Integer raceId, User currentUser) {
        ensureRaceExists(raceId);
        ensureAssignedReferee(raceId, currentUser);

        RaceMinute minute = raceMinuteRepository.findByRaceId(raceId)
                .orElseThrow(() -> new IllegalArgumentException("This race does not have minutes yet."));
        if (minute.getMinutesFileUrl() == null || minute.getMinutesFileUrl().isBlank()) {
            throw new IllegalArgumentException("minutesFileUrl is required before handing over to BTC.");
        }

        userRepository.findActiveOrganizersAndAdmins().forEach(user -> {
            Notification notification = new Notification();
            notification.setUserId(user.getUserId());
            notification.setTitle("Ket qua race san sang de BTC duyet");
            notification.setBody("Referee da ban giao ket qua va bien ban cho race #" + raceId + ".");
            notification.setNotifType("ResultReadyForReview");
            notification.setRelatedEntityId(raceId);
            notification.setRelatedEntity("Race");
            notification.setIsRead(false);
            notificationRepository.save(notification);
        });

        return toResponse(minute);
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || raceMinuteRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Race was not found.");
        }
    }

    private Integer ensureAssignedReferee(Integer raceId, User currentUser) {
        if (currentUser == null || currentUser.getRole() == null
                || !"Referee".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            throw new IllegalArgumentException("Only referees can create race minutes.");
        }
        if (raceMinuteRepository.countAssignedReferee(raceId, currentUser.getUserId()) == 0) {
            throw new IllegalArgumentException("Referee has not been assigned to this race.");
        }
        Integer refereeId = raceMinuteRepository.findRefereeIdByUserId(currentUser.getUserId());
        if (refereeId == null) {
            throw new IllegalArgumentException("Current user does not have a Referee profile.");
        }
        return refereeId;
    }

    private void validateRequest(RaceMinuteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Race minutes data is invalid.");
        }
        if (request.getMinutesFileUrl() == null || request.getMinutesFileUrl().isBlank()) {
            throw new IllegalArgumentException("minutesFileUrl is required.");
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
