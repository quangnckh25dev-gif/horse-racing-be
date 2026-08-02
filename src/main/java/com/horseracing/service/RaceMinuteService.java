package com.horseracing.service;

import com.horseracing.dto.RaceMinuteRequest;
import com.horseracing.dto.RaceMinuteResponse;
import com.horseracing.entity.Notification;
import com.horseracing.entity.RaceMinute;
import com.horseracing.entity.User;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceComplaintRepository;
import com.horseracing.repository.RaceMinuteRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RaceMinuteService {

    private static final long OWNER_COMPLAINT_WINDOW_SECONDS = 60;

    private final RaceMinuteRepository raceMinuteRepository;
    private final RaceComplaintRepository raceComplaintRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public RaceMinuteService(RaceMinuteRepository raceMinuteRepository,
                             RaceComplaintRepository raceComplaintRepository,
                             NotificationRepository notificationRepository,
                             UserRepository userRepository) {
        this.raceMinuteRepository = raceMinuteRepository;
        this.raceComplaintRepository = raceComplaintRepository;
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
        minute.setContent(trimToNull(request.getContent()));
        minute.setWeatherCondition(trimToNull(request.getWeatherCondition()));
        minute.setPreRaceChecks(request.getPreRaceChecks());
        minute.setPostRaceNotes(resolvePostRaceNotes(request));
        minute.setMinutesFileUrl(trimToNull(request.getMinutesFileUrl()));
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
        minute.setContent(trimToNull(request.getContent()));
        minute.setWeatherCondition(trimToNull(request.getWeatherCondition()));
        minute.setPreRaceChecks(request.getPreRaceChecks());
        minute.setPostRaceNotes(resolvePostRaceNotes(request));
        minute.setMinutesFileUrl(trimToNull(request.getMinutesFileUrl()));
        minute.setUpdatedAt(LocalDateTime.now());

        return toResponse(raceMinuteRepository.save(minute));
    }

    @Transactional
    public RaceMinuteResponse sendMinutesToOwners(Integer raceId, User currentUser) {
        ensureRaceExists(raceId);
        ensureAssignedReferee(raceId, currentUser);
        ensureRaceFinished(raceId);
        ensureRaceHasResults(raceId);

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
        ensureMinutesSentToOwners(minute);
        ensureComplaintWindowClosed(minute);
        ensureNoOpenComplaints(raceId);

        notifyOrganizerAndAdmins(raceId);

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
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("content is required.");
        }
        if (request.getWeatherCondition() == null || request.getWeatherCondition().isBlank()) {
            throw new IllegalArgumentException("weatherCondition is required.");
        }
        if (request.getMinutesFileUrl() == null || request.getMinutesFileUrl().isBlank()) {
            throw new IllegalArgumentException("minutesFileUrl is required.");
        }
    }

    private void ensureRaceFinished(Integer raceId) {
        String status = raceMinuteRepository.findRaceStatusByRaceId(raceId);
        if (!"Finished".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Race minutes can only be handed off after the race is Finished.");
        }
    }

    private void ensureRaceHasResults(Integer raceId) {
        if (raceMinuteRepository.countResultsByRaceId(raceId) == 0) {
            throw new IllegalArgumentException("Race results are required before handing off to Organizer.");
        }
    }

    private void ensureMinutesSentToOwners(RaceMinute minute) {
        if (!Boolean.TRUE.equals(minute.getSentToOwners()) || minute.getSentAt() == null) {
            throw new IllegalArgumentException("Send the race minutes to owners before handing off to Organizer.");
        }
    }

    private void ensureComplaintWindowClosed(RaceMinute minute) {
        LocalDateTime deadline = minute.getSentAt().plusSeconds(OWNER_COMPLAINT_WINDOW_SECONDS);
        if (LocalDateTime.now().isBefore(deadline)) {
            throw new IllegalArgumentException("Owner complaint window is still open. Please wait 1 minute after sending minutes to owners.");
        }
    }

    private void ensureNoOpenComplaints(Integer raceId) {
        if (raceComplaintRepository.countOpenComplaintsByRaceId(raceId) > 0) {
            throw new IllegalArgumentException("This race still has open complaints. Resolve or reject all complaints before handing off to Organizer.");
        }
    }

    private void notifyOrganizerAndAdmins(Integer raceId) {
        Integer organizerUserId = raceMinuteRepository.findOrganizerUserIdByRaceId(raceId);
        userRepository.findActiveOrganizersAndAdmins().stream()
                .filter(user -> "Admin".equalsIgnoreCase(user.getRole().getRoleName())
                        || user.getUserId().equals(organizerUserId))
                .forEach(user -> {
                    Notification notification = new Notification();
                    notification.setUserId(user.getUserId());
                    notification.setTitle("Race results are ready for review");
                    notification.setBody("The referee handed off race results and minutes for race #" + raceId + ".");
                    notification.setNotifType("ResultReadyForReview");
                    notification.setRelatedEntityId(raceId);
                    notification.setRelatedEntity("Race");
                    notification.setIsRead(false);
                    notificationRepository.save(notification);
                });
    }

    private String resolvePostRaceNotes(RaceMinuteRequest request) {
        if (request.getPostRaceNotes() != null) {
            return trimToNull(request.getPostRaceNotes());
        }

        return trimToNull(request.getNote());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RaceMinuteResponse toResponse(RaceMinute minute) {
        return new RaceMinuteResponse(
                minute.getMinuteId(),
                minute.getRaceId(),
                minute.getRefereeId(),
                minute.getContent(),
                minute.getWeatherCondition(),
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
