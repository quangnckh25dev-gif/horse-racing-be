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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

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
    private final WalletService walletService;

    public JockeyInvitationService(JockeyInvitationRepository invitationRepository,
                                   RaceEntryRepository raceEntryRepository,
                                   JockeyRepository jockeyRepository,
                                   HorseRepository horseRepository,
                                   HorseOwnerRepository horseOwnerRepository,
                                   RaceRepository raceRepository,
                                   UserRepository userRepository,
                                   NotificationRepository notificationRepository,
                                   CurrentUserService currentUserService,
                                   WalletService walletService) {
        this.invitationRepository = invitationRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.jockeyRepository = jockeyRepository;
        this.horseRepository = horseRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.raceRepository = raceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.walletService = walletService;
    }

    @Transactional
    public JockeyInvitationResponse sendInvitation(Integer entryId, JockeyInvitationRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        RaceEntry entry = getEntry(entryId);
        Horse horse = horseRepository.findById(entry.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));

        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("You do not have permission to invite a jockey for this entry.");
        }
        if (request == null || request.getJockeyId() == null) {
            throw new IllegalArgumentException("jockeyId is required.");
        }
        BigDecimal dealAmount = validateDealAmount(request.getDealAmount());
        if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Only organizer-approved entries can invite a jockey.");
        }
        if (Boolean.TRUE.equals(entry.getJockeyConfirmed()) || entry.getJockeyId() != null) {
            throw new IllegalArgumentException("This entry already has a confirmed jockey.");
        }
        if (invitationRepository.existsByEntryIdAndStatus(entryId, "Pending")) {
            throw new IllegalArgumentException("This entry already has a pending jockey invitation.");
        }

        Jockey jockey = jockeyRepository.findById(request.getJockeyId())
                .orElseThrow(() -> new IllegalArgumentException("Jockey was not found."));
        User jockeyUser = userRepository.findById(jockey.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Jockey user was not found."));
        if (!Boolean.TRUE.equals(jockeyUser.getIsActive())) {
            throw new IllegalArgumentException("Jockey is inactive.");
        }
        if (invitationRepository.existsByEntryIdAndJockeyIdAndStatus(entryId, jockey.getJockeyId(), "Pending")) {
            throw new IllegalArgumentException("This jockey already has a pending invitation.");
        }

        JockeyInvitation invitation = new JockeyInvitation();
        invitation.setEntryId(entryId);
        invitation.setJockeyId(jockey.getJockeyId());
        invitation.setInvitedByOwner(owner.getOwnerId());
        invitation.setDealAmount(dealAmount);
        invitation.setMessage(trimToNull(request.getMessage()));
        invitation.setStatus("Pending");

        JockeyInvitation saved = invitationRepository.save(invitation);
        notifyJockey(saved, saved.getMessage());
        return toResponse(saved);
    }

    public List<JockeyInvitationResponse> getReceivedInvitations(HttpServletRequest httpRequest, String status, String keyword) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Jockey jockey = jockeyRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a Jockey profile."));
        String statusFilter = normalizeOptionalInvitationStatus(status);
        String keywordFilter = trimToNull(keyword);
        return invitationRepository.findByJockeyIdOrderByInvitedAtDesc(jockey.getJockeyId())
                .stream()
                .filter(invitation -> matchesInvitationStatus(invitation, statusFilter))
                .filter(invitation -> matchesInvitationKeyword(invitation, keywordFilter))
                .map(this::toResponse)
                .toList();
    }

    public List<JockeyInvitationResponse> getSentInvitations(HttpServletRequest httpRequest, String status, String keyword) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        String statusFilter = normalizeOptionalInvitationStatus(status);
        String keywordFilter = trimToNull(keyword);
        return invitationRepository.findByInvitedByOwnerOrderByInvitedAtDesc(owner.getOwnerId())
                .stream()
                .filter(invitation -> matchesInvitationStatus(invitation, statusFilter))
                .filter(invitation -> matchesInvitationKeyword(invitation, keywordFilter))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public JockeyInvitationResponse respondInvitation(Integer invitationId, JockeyInvitationRespondRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Jockey jockey = jockeyRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a Jockey profile."));
        JockeyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation was not found."));

        if (!jockey.getJockeyId().equals(invitation.getJockeyId())) {
            throw new IllegalArgumentException("You do not have permission to respond to this invitation.");
        }
        if (!"Pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new IllegalArgumentException("This invitation has already been processed.");
        }
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required.");
        }

        String status = normalizeResponseStatus(request.getStatus());

        if ("Accepted".equals(status)) {
            RaceEntry entry = getEntry(invitation.getEntryId());
            if (!"Approved".equalsIgnoreCase(entry.getRegistrationStatus())) {
                throw new IllegalArgumentException("Only organizer-approved entries can accept invitations.");
            }
            if (Boolean.TRUE.equals(entry.getJockeyConfirmed()) || entry.getJockeyId() != null) {
                throw new IllegalArgumentException("This entry already has a confirmed jockey.");
            }
            Horse horse = horseRepository.findById(entry.getHorseId())
                    .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
            HorseOwner owner = horseOwnerRepository.findById(horse.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Horse owner was not found."));
            walletService.transferJockeyDeal(owner.getUserId(), jockey.getUserId(),
                    invitation.getDealAmount(), invitation.getInvitationId());
            entry.setJockeyId(jockey.getJockeyId());
            entry.setJockeyConfirmed(true);
            entry.setRegistrationStatus("Ready");
            raceEntryRepository.save(entry);
            invitation.setResponseReason(null);
        } else {
            String reason = trimToNull(request.getReason());
            if (reason == null) {
                throw new IllegalArgumentException("Reject reason is required.");
            }
            invitation.setResponseReason(reason);
        }

        invitation.setStatus(status);
        invitation.setRespondedAt(LocalDateTime.now());
        return toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public JockeyInvitationResponse cancelInvitation(Integer invitationId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        JockeyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation was not found."));

        if (!owner.getOwnerId().equals(invitation.getInvitedByOwner())) {
            throw new IllegalArgumentException("You do not have permission to cancel this invitation.");
        }
        if (!"Pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new IllegalArgumentException("Only pending invitations can be cancelled.");
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
        throw new IllegalArgumentException("status only accepts Accepted or Declined.");
    }

    private BigDecimal validateDealAmount(BigDecimal dealAmount) {
        if (dealAmount == null || dealAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("dealAmount must be greater than 0.");
        }
        return dealAmount;
    }

    private String normalizeOptionalInvitationStatus(String status) {
        String cleanStatus = trimToNull(status);
        if (cleanStatus == null) {
            return null;
        }
        if ("Rejected".equalsIgnoreCase(cleanStatus) || "Reject".equalsIgnoreCase(cleanStatus)) {
            return "Declined";
        }
        if ("Accept".equalsIgnoreCase(cleanStatus)) {
            return "Accepted";
        }
        if (!"Pending".equalsIgnoreCase(cleanStatus)
                && !"Accepted".equalsIgnoreCase(cleanStatus)
                && !"Declined".equalsIgnoreCase(cleanStatus)
                && !"Cancelled".equalsIgnoreCase(cleanStatus)) {
            throw new IllegalArgumentException("status only accepts Pending, Accepted, Declined, or Cancelled.");
        }
        return cleanStatus;
    }

    private boolean matchesInvitationStatus(JockeyInvitation invitation, String status) {
        return status == null || status.equalsIgnoreCase(invitation.getStatus());
    }

    private boolean matchesInvitationKeyword(JockeyInvitation invitation, String keyword) {
        if (keyword == null) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        RaceEntry entry = raceEntryRepository.findById(invitation.getEntryId()).orElse(null);
        Race race = entry == null ? null : raceRepository.findById(entry.getRaceId()).orElse(null);
        Horse horse = entry == null ? null : horseRepository.findById(entry.getHorseId()).orElse(null);
        HorseOwner owner = horse == null ? null : horseOwnerRepository.findById(horse.getOwnerId()).orElse(null);
        User ownerUser = owner == null ? null : userRepository.findById(owner.getUserId()).orElse(null);
        Jockey jockey = jockeyRepository.findById(invitation.getJockeyId()).orElse(null);
        User jockeyUser = jockey == null ? null : userRepository.findById(jockey.getUserId()).orElse(null);
        return contains(invitation.getMessage(), normalized)
                || contains(invitation.getResponseReason(), normalized)
                || contains(race == null ? null : race.getRaceName(), normalized)
                || contains(horse == null ? null : horse.getHorseName(), normalized)
                || contains(ownerUser == null ? null : ownerUser.getFullName(), normalized)
                || contains(ownerUser == null ? null : ownerUser.getUsername(), normalized)
                || contains(jockeyUser == null ? null : jockeyUser.getFullName(), normalized)
                || contains(jockeyUser == null ? null : jockeyUser.getUsername(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RaceEntry getEntry(Integer entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId is required.");
        }
        return raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry was not found."));
    }

    private HorseOwner getOwnerByUserId(Integer userId) {
        return horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a HorseOwner profile."));
    }

    private void notifyJockey(JockeyInvitation invitation, String message) {
        Jockey jockey = jockeyRepository.findById(invitation.getJockeyId()).orElse(null);
        if (jockey == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(jockey.getUserId());
        notification.setTitle("You received a race invitation");
        notification.setBody(message == null || message.isBlank() ? "A horse owner invited you to join a race." : message);
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
                invitation.getDealAmount(),
                invitation.getMessage(),
                invitation.getStatus(),
                invitation.getResponseReason(),
                invitation.getInvitedAt(),
                invitation.getRespondedAt()
        );
    }
}
