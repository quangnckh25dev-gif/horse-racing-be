package com.horseracing.service;

import com.horseracing.dto.JockeyInvitationRequest;
import com.horseracing.dto.JockeyInvitationRespondRequest;
import com.horseracing.dto.JockeyInvitationResponse;
import com.horseracing.entity.Horse;
import com.horseracing.entity.HorseOwner;
import com.horseracing.entity.Jockey;
import com.horseracing.entity.JockeyInvitation;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyInvitationRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JockeyInvitationService {

    private final JockeyInvitationRepository invitationRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final JockeyRepository jockeyRepository;
    private final HorseRepository horseRepository;
    private final HorseOwnerRepository horseOwnerRepository;
    private final RaceRepository raceRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public JockeyInvitationService(JockeyInvitationRepository invitationRepository,
                                   RaceEntryRepository raceEntryRepository,
                                   JockeyRepository jockeyRepository,
                                   HorseRepository horseRepository,
                                   HorseOwnerRepository horseOwnerRepository,
                                   RaceRepository raceRepository,
                                   UserRepository userRepository,
                                   NotificationRepository notificationRepository,
                                   CurrentUserService currentUserService) {
        this.invitationRepository = invitationRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.jockeyRepository = jockeyRepository;
        this.horseRepository = horseRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.raceRepository = raceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public JockeyInvitationResponse sendInvitation(Integer entryId, JockeyInvitationRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        RaceEntry entry = getEntry(entryId);
        Horse horse = horseRepository.findById(entry.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay horse"));

        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("Ban khong co quyen moi jockey cho entry nay");
        }
        if (request == null || request.getJockeyId() == null) {
            throw new IllegalArgumentException("jockeyId khong duoc de trong");
        }
        if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Chi entry da duoc BTC duyet moi duoc moi jockey");
        }
        if (Boolean.TRUE.equals(entry.getJockeyConfirmed()) || entry.getJockeyId() != null) {
            throw new IllegalArgumentException("Entry nay da co jockey xac nhan");
        }
        if (invitationRepository.existsByEntryIdAndStatus(entryId, "Pending")) {
            throw new IllegalArgumentException("Entry nay dang co loi moi jockey cho phan hoi");
        }

        Jockey jockey = jockeyRepository.findById(request.getJockeyId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay jockey"));
        User jockeyUser = userRepository.findById(jockey.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user cua jockey"));
        if (!Boolean.TRUE.equals(jockeyUser.getIsActive())) {
            throw new IllegalArgumentException("Jockey dang inactive");
        }
        if (invitationRepository.existsByEntryIdAndJockeyIdAndStatus(entryId, jockey.getJockeyId(), "Pending")) {
            throw new IllegalArgumentException("Da co loi moi Pending cho jockey nay");
        }

        JockeyInvitation invitation = new JockeyInvitation();
        invitation.setEntryId(entryId);
        invitation.setJockeyId(jockey.getJockeyId());
        invitation.setInvitedByOwner(owner.getOwnerId());
        invitation.setStatus("Pending");

        JockeyInvitation saved = invitationRepository.save(invitation);
        notifyJockey(saved, request.getMessage());
        return toResponse(saved);
    }

    public List<JockeyInvitationResponse> getReceivedInvitations(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Jockey jockey = jockeyRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User hien tai chua co ho so Jockey"));
        return invitationRepository.findByJockeyIdOrderByInvitedAtDesc(jockey.getJockeyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<JockeyInvitationResponse> getSentInvitations(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        return invitationRepository.findByInvitedByOwnerOrderByInvitedAtDesc(owner.getOwnerId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public JockeyInvitationResponse respondInvitation(Integer invitationId, JockeyInvitationRespondRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Jockey jockey = jockeyRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User hien tai chua co ho so Jockey"));
        JockeyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay invitation"));

        if (!jockey.getJockeyId().equals(invitation.getJockeyId())) {
            throw new IllegalArgumentException("Ban khong co quyen phan hoi invitation nay");
        }
        if (!"Pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new IllegalArgumentException("Invitation da duoc xu ly");
        }
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status khong duoc de trong");
        }

        String status = normalizeResponseStatus(request.getStatus());
        invitation.setStatus(status);
        invitation.setRespondedAt(LocalDateTime.now());

        if ("Accepted".equals(status)) {
            RaceEntry entry = getEntry(invitation.getEntryId());
            if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())) {
                throw new IllegalArgumentException("Chi entry da duoc BTC duyet moi duoc nhan loi moi");
            }
            if (Boolean.TRUE.equals(entry.getJockeyConfirmed()) || entry.getJockeyId() != null) {
                throw new IllegalArgumentException("Entry nay da co jockey xac nhan");
            }
            entry.setJockeyId(jockey.getJockeyId());
            entry.setJockeyConfirmed(true);
            entry.setRegistrationStatus("Ready");
            raceEntryRepository.save(entry);
        }

        return toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public JockeyInvitationResponse cancelInvitation(Integer invitationId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        JockeyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay invitation"));

        if (!owner.getOwnerId().equals(invitation.getInvitedByOwner())) {
            throw new IllegalArgumentException("Ban khong co quyen huy invitation nay");
        }
        if (!"Pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new IllegalArgumentException("Chi loi moi dang cho phan hoi moi huy duoc");
        }

        invitation.setStatus("Cancelled");
        invitation.setRespondedAt(LocalDateTime.now());
        return toResponse(invitationRepository.save(invitation));
    }

    private String normalizeResponseStatus(String status) {
        if ("Accepted".equalsIgnoreCase(status) || "Accept".equalsIgnoreCase(status)) {
            return "Accepted";
        }
        if ("Declined".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status) || "Reject".equalsIgnoreCase(status)) {
            return "Declined";
        }
        throw new IllegalArgumentException("status chi chap nhan Accepted hoac Declined");
    }

    private RaceEntry getEntry(Integer entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId khong duoc de trong");
        }
        return raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay entry"));
    }

    private HorseOwner getOwnerByUserId(Integer userId) {
        return horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User hien tai chua co ho so HorseOwner"));
    }

    private void notifyJockey(JockeyInvitation invitation, String message) {
        Jockey jockey = jockeyRepository.findById(invitation.getJockeyId()).orElse(null);
        if (jockey == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(jockey.getUserId());
        notification.setTitle("Ban nhan duoc loi moi tham gia race");
        notification.setBody(message == null || message.isBlank() ? "Mot horse owner da moi ban tham gia race." : message);
        notification.setNotifType("InvitationReceived");
        notification.setRelatedEntityId(invitation.getInvitationId());
        notification.setRelatedEntity("Invitation");
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private JockeyInvitationResponse toResponse(JockeyInvitation invitation) {
        RaceEntry entry = raceEntryRepository.findById(invitation.getEntryId()).orElse(null);
        Race race = entry == null ? null : raceRepository.findById(entry.getRaceId()).orElse(null);
        Horse horse = entry == null ? null : horseRepository.findById(entry.getHorseId()).orElse(null);
        HorseOwner owner = horse == null ? null : horseOwnerRepository.findById(horse.getOwnerId()).orElse(null);
        User ownerUser = owner == null ? null : userRepository.findById(owner.getUserId()).orElse(null);
        Jockey jockey = jockeyRepository.findById(invitation.getJockeyId()).orElse(null);
        User jockeyUser = jockey == null ? null : userRepository.findById(jockey.getUserId()).orElse(null);

        return new JockeyInvitationResponse(
                invitation.getInvitationId(),
                invitation.getEntryId(),
                entry == null ? null : entry.getRaceId(),
                race == null ? null : race.getRaceName(),
                horse == null ? null : horse.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                invitation.getJockeyId(),
                jockeyUser == null ? null : jockeyUser.getFullName(),
                invitation.getInvitedByOwner(),
                ownerUser == null ? null : ownerUser.getFullName(),
                invitation.getStatus(),
                invitation.getInvitedAt(),
                invitation.getRespondedAt()
        );
    }
}
