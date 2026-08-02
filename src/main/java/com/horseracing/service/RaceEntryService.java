package com.horseracing.service;

import com.horseracing.dto.RaceEntryApproveRequest;
import com.horseracing.dto.RaceEntryRequest;
import com.horseracing.dto.RaceEntryResponse;
import com.horseracing.entity.Horse;
import com.horseracing.dto.HorseHealthRecordResponse;
import com.horseracing.entity.HorseHealthRecord;
import com.horseracing.entity.HorseOwner;
import com.horseracing.entity.Jockey;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.HorseHealthRecordRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import com.horseracing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RaceEntryService {

    private final RaceEntryRepository raceEntryRepository;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final HorseHealthRecordRepository healthRecordRepository;
    private final HorseOwnerRepository horseOwnerRepository;
    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;

    public RaceEntryService(RaceEntryRepository raceEntryRepository, RaceRepository raceRepository,
                            HorseRepository horseRepository, HorseHealthRecordRepository healthRecordRepository,
                            HorseOwnerRepository horseOwnerRepository,
                            JockeyRepository jockeyRepository, UserRepository userRepository,
                            NotificationRepository notificationRepository, CurrentUserService currentUserService,
                            TournamentRepository tournamentRepository, RoundRepository roundRepository) {
        this.raceEntryRepository = raceEntryRepository;
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.jockeyRepository = jockeyRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.tournamentRepository = tournamentRepository;
        this.roundRepository = roundRepository;
    }

    public List<RaceEntryResponse> getRaceEntries(Integer raceId) {
        ensureRaceExists(raceId);
        return raceEntryRepository.findByRaceId(raceId).stream().map(this::toResponse).toList();
    }

    public List<RaceEntryResponse> getPublicRaceEntries(Integer raceId) {
        ensureRaceExists(raceId);
        return raceEntryRepository.findPublicEntriesByRaceId(raceId).stream().map(this::toResponse).toList();
    }

    public List<RaceEntryResponse> getMyEntries(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        return raceEntryRepository.findByOwnerId(owner.getOwnerId()).stream().map(this::toResponse).toList();
    }

    public List<RaceEntryResponse> getMyApprovedEntries(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        return raceEntryRepository.findApprovedByOwnerId(owner.getOwnerId()).stream().map(this::toResponse).toList();
    }

    public RaceEntryResponse registerHorse(Integer raceId, RaceEntryRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        Race race = getRace(raceId);
        validateRaceCanReceiveEntry(race);

        if (request == null || request.getHorseId() == null) {
            throw new IllegalArgumentException("horseId is required.");
        }

        Horse horse = horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
        if (Boolean.TRUE.equals(horse.getIsDeleted())) {
            throw new IllegalArgumentException("Horse was archived.");
        }
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("This horse does not belong to you.");
        }
        if (!Boolean.TRUE.equals(horse.getIsActive())) {
            throw new IllegalArgumentException("Horse is inactive.");
        }
        ensureHorseCanEnterRound(race, horse.getHorseId());
        if (raceEntryRepository.existsActiveRegistration(raceId, horse.getHorseId())) {
            throw new IllegalArgumentException("Horse has already registered for this race.");
        }

        long currentEntries = raceEntryRepository.countActiveRegistrations(raceId);
        if (race.getMaxParticipants() != null && currentEntries >= race.getMaxParticipants()) {
            throw new IllegalArgumentException("Race has reached the maximum number of horse participants.");
        }

        RaceEntry entry = raceEntryRepository.findReusableRegistration(raceId, horse.getHorseId())
                .orElseGet(RaceEntry::new);
        entry.setRaceId(raceId);
        entry.setHorseId(horse.getHorseId());
        entry.setJockeyId(null);
        entry.setLaneNumber(request.getLaneNumber());
        entry.setOrganizerApproved(false);
        entry.setApprovedBy(null);
        entry.setRejectReason(null);
        entry.setRegistrationStatus("Pending");
        entry.setJockeyConfirmed(false);
        entry.setRoundStatus(null);
        entry.setEliminationRoundId(null);
        entry.setEliminationReason(null);
        entry.setOdds(BigDecimal.valueOf(2));

        RaceEntry saved = raceEntryRepository.save(entry);
        notifyOrganizers(saved, race, horse, user);
        return toResponse(saved);
    }

    public RaceEntryResponse withdrawEntry(Integer raceId, Integer entryId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        RaceEntry entry = getEntryInRace(raceId, entryId);
        Horse horse = horseRepository.findById(entry.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
        HorseOwner owner = getOwnerByUserId(user.getUserId());

        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("You do not have permission to withdraw this entry.");
        }
        if ("Approved".equalsIgnoreCase(entry.getRegistrationStatus())
                || "Ready".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Approved entries cannot be withdrawn.");
        }

        entry.setRegistrationStatus("Withdrawn");
        entry.setRejectReason(null);
        return toResponse(raceEntryRepository.save(entry));
    }

    public RaceEntryResponse approveEntry(Integer raceId, Integer entryId, RaceEntryApproveRequest request,
                                          HttpServletRequest httpRequest) {
        // Organizer approves or rejects a race entry.
        User user = currentUserService.getCurrentUser(httpRequest);
        requireOrganizer(user);
        Race race = getRace(raceId);
        tournamentRepository.findByTournamentIdAndCreatedBy(race.getTournamentId(), user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Race does not belong to the current organizer."));

        RaceEntry entry = getEntryInRace(raceId, entryId);
        if (request == null || request.getApproved() == null) {
            throw new IllegalArgumentException("approved is required.");
        }
        if ("Withdrawn".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Withdrawn entries cannot be approved.");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            Horse horse = horseRepository.findById(entry.getHorseId())
                    .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
            if (!Boolean.TRUE.equals(horse.getIsActive())) {
                throw new IllegalArgumentException("Inactive horses cannot be approved.");
            }
            String hs = horse.getHealthStatus() == null ? "" : horse.getHealthStatus().trim();
            if (!isActiveHorseStatus(hs)) {
                throw new IllegalArgumentException("Only horses with healthStatus = Active can be approved.");
            }
            entry.setRegistrationStatus("Approved");
            entry.setOrganizerApproved(true);
            entry.setApprovedBy(user.getUserId());
            entry.setRejectReason(null);
        } else {
            String reason = normalizeReason(request.getReason());
            if (reason == null) {
                throw new IllegalArgumentException("Reject reason is required.");
            }
            entry.setRegistrationStatus("Rejected");
            entry.setOrganizerApproved(false);
            entry.setApprovedBy(user.getUserId());
            entry.setRejectReason(reason);
        }

        RaceEntry saved = raceEntryRepository.save(entry);
        notifyOwner(saved, Boolean.TRUE.equals(request.getApproved()), saved.getRejectReason());
        return toResponse(saved);
    }

    public RaceEntryResponse getEntry(Integer entryId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry was not found."));
        ensureCanViewEntry(user, entry);
        return toResponse(entry);
    }

    private void ensureHorseCanEnterRound(Race race, Integer horseId) {
        if (race.getRoundId() == null) {
            return;
        }
        Round round = roundRepository.findById(race.getRoundId())
                .orElseThrow(() -> new IllegalArgumentException("Round was not found."));
        if (round.getRoundOrder() == null || round.getRoundOrder() <= 1) {
            return;
        }
        if (raceEntryRepository.countEliminatedBeforeRound(race.getTournamentId(), horseId, round.getRoundOrder()) > 0) {
            throw new IllegalArgumentException("This horse was eliminated in a previous round.");
        }
    }

    private void validateRaceCanReceiveEntry(Race race) {
        if (!"RegistrationOpen".equalsIgnoreCase(race.getStatus())) {
            throw new IllegalArgumentException("Race registration is not open.");
        }
        if (race.getRoundId() != null) {
            Round round = roundRepository.findById(race.getRoundId())
                    .orElseThrow(() -> new IllegalArgumentException("Round was not found."));
            if (round.getRoundOrder() != null && round.getRoundOrder() > 1) {
                throw new IllegalArgumentException("Semi Final and Final entries are advanced automatically from the previous round.");
            }
        }
        Tournament tournament = tournamentRepository.findById(race.getTournamentId())
                .orElseThrow(() -> new IllegalArgumentException("Tournament was not found."));
        if (!Set.of("Draft", "Open", "Ongoing").contains(tournament.getStatus())) {
            throw new IllegalArgumentException("Tournament is not open for registration.");
        }
    }

    private Race getRace(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId is required.");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race was not found."));
    }

    private void ensureRaceExists(Integer raceId) {
        getRace(raceId);
    }

    private RaceEntry getEntryInRace(Integer raceId, Integer entryId) {
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry was not found."));
        if (!entry.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("This entry does not belong to the selected race.");
        }
        return entry;
    }

    private HorseOwner getOwnerByUserId(Integer userId) {
        return horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a HorseOwner profile."));
    }

    private void ensureCanViewEntry(User user, RaceEntry entry) {
        if (isOrganizer(user)) {
            Race race = getRace(entry.getRaceId());
            tournamentRepository.findByTournamentIdAndCreatedBy(race.getTournamentId(), user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Race does not belong to the current organizer."));
            return;
        }

        Horse horse = horseRepository.findById(entry.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
        HorseOwner owner = getOwnerByUserId(user.getUserId());
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("You do not have permission to view this entry.");
        }
    }

    private void notifyOwner(RaceEntry entry, boolean approved, String reason) {
        Horse horse = horseRepository.findById(entry.getHorseId()).orElse(null);
        if (horse == null) {
            return;
        }
        HorseOwner owner = horseOwnerRepository.findById(horse.getOwnerId()).orElse(null);
        if (owner == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(owner.getUserId());
        notification.setTitle(approved ? "Race entry approved" : "Race entry rejected");
        notification.setBody(reason == null || reason.isBlank() ? null : reason);
        notification.setNotifType(approved ? "EntryApproved" : "EntryRejected");
        notification.setRelatedEntityId(entry.getEntryId());
        notification.setRelatedEntity("RaceEntry");
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private boolean isActiveHorseStatus(String status) {
        String normalized = Normalizer.normalize(status == null ? "" : status.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return "active".equals(normalized) || "hoat dong".equals(normalized);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }

    private void notifyOrganizers(RaceEntry entry, Race race, Horse horse, User ownerUser) {
        String title = "New race entry pending approval";
        String body = ownerUser.getFullName() + " registered horse "
                + horse.getHorseName() + " for race " + race.getRaceName() + ".";

        Tournament tournament = tournamentRepository.findById(race.getTournamentId()).orElse(null);
        if (tournament == null || tournament.getCreatedBy() == null) {
            return;
        }
        userRepository.findById(tournament.getCreatedBy()).stream().forEach(user -> {
            Notification notification = new Notification();
            notification.setUserId(user.getUserId());
            notification.setTitle(title);
            notification.setBody(body);
            notification.setNotifType("EntryPendingApproval");
            notification.setRelatedEntityId(entry.getEntryId());
            notification.setRelatedEntity("RaceEntry");
            notification.setIsRead(false);
            notificationRepository.save(notification);
        });
    }

    private RaceEntryResponse toResponse(RaceEntry entry) {
        Race race = raceRepository.findById(entry.getRaceId()).orElse(null);
        Horse horse = horseRepository.findById(entry.getHorseId()).orElse(null);
        HorseOwner owner = horse == null ? null : horseOwnerRepository.findById(horse.getOwnerId()).orElse(null);
        User ownerUser = owner == null ? null : userRepository.findById(owner.getUserId()).orElse(null);
        Jockey jockey = entry.getJockeyId() == null ? null : jockeyRepository.findById(entry.getJockeyId()).orElse(null);
        User jockeyUser = jockey == null ? null : userRepository.findById(jockey.getUserId()).orElse(null);

        return new RaceEntryResponse(
                entry.getEntryId(),
                entry.getRaceId(),
                race == null ? null : race.getRaceName(),
                entry.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                owner == null ? null : owner.getOwnerId(),
                ownerUser == null ? null : ownerUser.getFullName(),
                entry.getJockeyId(),
                jockeyUser == null ? null : jockeyUser.getFullName(),
                entry.getLaneNumber(),
                entry.getRegistrationStatus(),
                entry.getOrganizerApproved(),
                entry.getApprovedBy(),
                entry.getRejectReason(),
                entry.getRoundStatus(),
                entry.getEliminationRoundId(),
                entry.getEliminationReason(),
                entry.getJockeyConfirmed(),
                entry.getOdds(),
                horse == null ? null : horse.getHealthStatus(),
                horse == null ? List.of() : healthRecordRepository.findByHorseIdOrderByCheckDateDesc(horse.getHorseId())
                        .stream()
                        .map(this::toHealthResponse)
                        .toList(),
                entry.getRegisteredAt(),
                entry.getUpdatedAt()
        );
    }

    private HorseHealthRecordResponse toHealthResponse(HorseHealthRecord record) {
        return new HorseHealthRecordResponse(
                record.getRecordId(),
                record.getHorseId(),
                record.getCheckDate(),
                record.getVetName(),
                record.getHealthStatus(),
                record.getDiagnosis(),
                record.getNotes(),
                record.getEvidenceUrl(),
                record.getStatus(),
                record.getSubmittedBy(),
                record.getRecordedBy(),
                record.getReviewedBy(),
                record.getReviewedAt(),
                record.getReviewNote(),
                record.getCreatedAt()
        );
    }

    private void requireOrganizer(User user) {
        if (user == null || user.getRole() == null || !"Organizer".equalsIgnoreCase(user.getRole().getRoleName())
                || !Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Only organizers can approve entries.");
        }
    }

    private boolean isOrganizer(User user) {
        return user != null
                && user.getRole() != null
                && "Organizer".equalsIgnoreCase(user.getRole().getRoleName())
                && Boolean.TRUE.equals(user.getIsActive())
                && Boolean.TRUE.equals(user.getIsApproved());
    }
}
