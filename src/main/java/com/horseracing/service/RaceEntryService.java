package com.horseracing.service;

import com.horseracing.dto.RaceEntryApproveRequest;
import com.horseracing.dto.RaceEntryRequest;
import com.horseracing.dto.RaceEntryResponse;
import com.horseracing.entity.Horse;
import com.horseracing.entity.HorseOwner;
import com.horseracing.entity.Jockey;
import com.horseracing.entity.Notification;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.TournamentRepository;
import com.horseracing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RaceEntryService {

    private final RaceEntryRepository raceEntryRepository;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final HorseOwnerRepository horseOwnerRepository;
    private final JockeyRepository jockeyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final TournamentRepository tournamentRepository;

    public RaceEntryService(RaceEntryRepository raceEntryRepository, RaceRepository raceRepository,
                            HorseRepository horseRepository, HorseOwnerRepository horseOwnerRepository,
                            JockeyRepository jockeyRepository, UserRepository userRepository,
                            NotificationRepository notificationRepository, CurrentUserService currentUserService,
                            TournamentRepository tournamentRepository) {
        this.raceEntryRepository = raceEntryRepository;
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.jockeyRepository = jockeyRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.tournamentRepository = tournamentRepository;
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
            throw new IllegalArgumentException("horseId khong duoc de trong");
        }

        Horse horse = horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay horse"));
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("Horse khong thuoc so huu cua ban");
        }
        if (!Boolean.TRUE.equals(horse.getIsActive())) {
            throw new IllegalArgumentException("Horse dang inactive");
        }
        if (raceEntryRepository.existsActiveRegistration(raceId, horse.getHorseId())) {
            throw new IllegalArgumentException("Horse da dang ky race nay");
        }

        long currentEntries = raceEntryRepository.countActiveRegistrations(raceId);
        if (race.getMaxParticipants() != null && currentEntries >= race.getMaxParticipants()) {
            throw new IllegalArgumentException("Race da du so luong horse tham gia");
        }

        RaceEntry entry = new RaceEntry();
        entry.setRaceId(raceId);
        entry.setHorseId(horse.getHorseId());
        entry.setLaneNumber(request.getLaneNumber());
        entry.setOrganizerApproved(false);
        entry.setRegistrationStatus("Pending");
        entry.setJockeyConfirmed(false);
        entry.setOdds(BigDecimal.valueOf(2));

        RaceEntry saved = raceEntryRepository.save(entry);
        notifyOrganizers(saved, race, horse, user);
        return toResponse(saved);
    }

    public RaceEntryResponse withdrawEntry(Integer raceId, Integer entryId, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        RaceEntry entry = getEntryInRace(raceId, entryId);
        Horse horse = horseRepository.findById(entry.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay horse"));
        HorseOwner owner = getOwnerByUserId(user.getUserId());

        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("Ban khong co quyen rut entry nay");
        }
        if ("Approved".equalsIgnoreCase(entry.getRegistrationStatus())
                || "Ready".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Entry da duoc duyet, khong the rut");
        }

        entry.setRegistrationStatus("Withdrawn");
        entry.setRejectReason(null);
        return toResponse(raceEntryRepository.save(entry));
    }

    public RaceEntryResponse approveEntry(Integer raceId, Integer entryId, RaceEntryApproveRequest request,
                                          HttpServletRequest httpRequest) {
        //của buiquangann
        User user = currentUserService.getCurrentUser(httpRequest);
        requireOrganizer(user);
        Race race = getRace(raceId);
        tournamentRepository.findByTournamentIdAndCreatedBy(race.getTournamentId(), user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Race khong thuoc Organizer hien tai"));

        RaceEntry entry = getEntryInRace(raceId, entryId);
        if (request == null || request.getApproved() == null) {
            throw new IllegalArgumentException("approved khong duoc de trong");
        }
        if ("Withdrawn".equalsIgnoreCase(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Entry da rut, khong the duyet");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            Horse horse = horseRepository.findById(entry.getHorseId())
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay horse"));
            // Chấp nhận cả 'Active' lẫn 'Hoạt động' — đồng bộ với HorseService.normalizeStatus. KHONG XOA!
            String hs = horse.getHealthStatus() == null ? "" : horse.getHealthStatus().trim();
            if (!"Hoạt động".equals(hs) && !"Active".equalsIgnoreCase(hs) && !"Hoat dong".equalsIgnoreCase(hs)) {
                throw new IllegalArgumentException("Chi duyet ngua co healthStatus = Hoat dong");
            }
            entry.setRegistrationStatus("Approved");
            entry.setOrganizerApproved(true);
            entry.setApprovedBy(user.getUserId());
            entry.setRejectReason(null);
        } else {
            entry.setRegistrationStatus("Rejected");
            entry.setOrganizerApproved(false);
            entry.setApprovedBy(user.getUserId());
            entry.setRejectReason(request.getReason());
        }

        RaceEntry saved = raceEntryRepository.save(entry);
        notifyOwner(saved, Boolean.TRUE.equals(request.getApproved()), request.getReason());
        return toResponse(saved);
    }

    public RaceEntryResponse getEntry(Integer entryId) {
        return toResponse(raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay entry")));
    }

    private void validateRaceCanReceiveEntry(Race race) {
        if (!"RegistrationOpen".equalsIgnoreCase(race.getStatus())) {
            throw new IllegalArgumentException("Race chua mo dang ky");
        }
        Tournament tournament = tournamentRepository.findById(race.getTournamentId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament"));
        if (!"Open".equalsIgnoreCase(tournament.getStatus())) {
            throw new IllegalArgumentException("Tournament chua duoc Admin duyet mo dang ky");
        }
    }

    private Race getRace(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId khong duoc de trong");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race"));
    }

    private void ensureRaceExists(Integer raceId) {
        getRace(raceId);
    }

    private RaceEntry getEntryInRace(Integer raceId, Integer entryId) {
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay entry"));
        if (!entry.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("Entry khong thuoc race nay");
        }
        return entry;
    }

    private HorseOwner getOwnerByUserId(Integer userId) {
        return horseOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User hien tai chua co ho so HorseOwner"));
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
        notification.setTitle(approved ? "Dang ky race da duoc duyet" : "Dang ky race bi tu choi");
        notification.setBody(reason == null || reason.isBlank() ? null : reason);
        notification.setNotifType(approved ? "EntryApproved" : "EntryRejected");
        notification.setRelatedEntityId(entry.getEntryId());
        notification.setRelatedEntity("RaceEntry");
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private void notifyOrganizers(RaceEntry entry, Race race, Horse horse, User ownerUser) {
        String title = "Co dang ky thi dau moi cho duyet";
        String body = ownerUser.getFullName() + " da dang ky ngua "
                + horse.getHorseName() + " vao race " + race.getRaceName() + ".";

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
                entry.getJockeyConfirmed(),
                entry.getOdds(),
                horse == null ? null : horse.getHealthStatus(),
                entry.getRegisteredAt(),
                entry.getUpdatedAt()
        );
    }

    private void requireOrganizer(User user) {
        if (user == null || user.getRole() == null || !"Organizer".equalsIgnoreCase(user.getRole().getRoleName())
                || !Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Chi Organizer moi duoc duyet entry");
        }
    }
}
