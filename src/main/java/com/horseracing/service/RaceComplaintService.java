package com.horseracing.service;

import com.horseracing.dto.RaceComplaintRequest;
import com.horseracing.dto.RaceComplaintResponse;
import com.horseracing.entity.Horse;
import com.horseracing.entity.HorseOwner;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceComplaint;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.Referee;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseOwnerRepository;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.RaceComplaintRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceResultRepository;
import com.horseracing.repository.RefereeRepository;
import com.horseracing.repository.TournamentRepository;
import com.horseracing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RaceComplaintService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_RESOLVED = "Resolved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_FORWARDED = "Forwarded";

    private final RaceComplaintRepository raceComplaintRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceResultRepository raceResultRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final RefereeRepository refereeRepository;
    private final HorseRepository horseRepository;
    private final HorseOwnerRepository horseOwnerRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public RaceComplaintService(RaceComplaintRepository raceComplaintRepository,
                                RaceRepository raceRepository,
                                RaceEntryRepository raceEntryRepository,
                                RaceResultRepository raceResultRepository,
                                RaceRefereeRepository raceRefereeRepository,
                                RefereeRepository refereeRepository,
                                HorseRepository horseRepository,
                                HorseOwnerRepository horseOwnerRepository,
                                TournamentRepository tournamentRepository,
                                UserRepository userRepository,
                                CurrentUserService currentUserService) {
        this.raceComplaintRepository = raceComplaintRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.raceResultRepository = raceResultRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.refereeRepository = refereeRepository;
        this.horseRepository = horseRepository;
        this.horseOwnerRepository = horseOwnerRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public RaceComplaintResponse createComplaint(RaceComplaintRequest request, HttpServletRequest httpRequest) {
        User ownerUser = currentUserService.getCurrentUser(httpRequest);
        requireRole(ownerUser, "HorseOwner");
        if (request == null) {
            throw new IllegalArgumentException("Complaint data is required.");
        }
        Race race = getRace(request.getRaceId());
        RaceEntry entry = getEntryInRace(race.getRaceId(), request.getEntryId());
        ensureRaceCanBeComplained(race);
        ensureOwnerOwnsEntry(ownerUser, entry);
        String reason = trimToNull(request.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("reason is required.");
        }

        RaceComplaint complaint = new RaceComplaint();
        complaint.setOwnerUserId(ownerUser.getUserId());
        complaint.setRaceId(race.getRaceId());
        complaint.setEntryId(entry.getEntryId());
        complaint.setReason(reason);
        complaint.setEvidenceUrl(trimToNull(request.getEvidenceUrl()));
        complaint.setStatus(STATUS_PENDING);
        return toResponse(raceComplaintRepository.save(complaint));
    }

    public List<RaceComplaintResponse> getMyComplaints(HttpServletRequest request) {
        User ownerUser = currentUserService.getCurrentUser(request);
        requireRole(ownerUser, "HorseOwner");
        return raceComplaintRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUser.getUserId())
                .stream().map(this::toResponse).toList();
    }

    public List<RaceComplaintResponse> getRefereeComplaints(HttpServletRequest request) {
        Referee referee = getCurrentReferee(request);
        return raceComplaintRepository.findAssignedToReferee(referee.getRefereeId())
                .stream().map(this::toResponse).toList();
    }

    public RaceComplaintResponse getRefereeComplaint(Integer id, HttpServletRequest request) {
        Referee referee = getCurrentReferee(request);
        RaceComplaint complaint = getComplaint(id);
        ensureRefereeAssigned(complaint.getRaceId(), referee.getRefereeId());
        return toResponse(complaint);
    }

    @Transactional
    public RaceComplaintResponse refereeResolve(Integer id, RaceComplaintRequest request, HttpServletRequest httpRequest) {
        Referee referee = getCurrentReferee(httpRequest);
        RaceComplaint complaint = getPendingComplaintForReferee(id, referee.getRefereeId());
        complaint.setStatus(STATUS_RESOLVED);
        complaint.setRefereeId(referee.getRefereeId());
        complaint.setRefereeNote(trimToNull(request == null ? null : request.getRefereeNote()));
        complaint.setResolvedAt(LocalDateTime.now());
        return toResponse(raceComplaintRepository.save(complaint));
    }

    @Transactional
    public RaceComplaintResponse refereeReject(Integer id, RaceComplaintRequest request, HttpServletRequest httpRequest) {
        Referee referee = getCurrentReferee(httpRequest);
        String note = requireNote(request == null ? null : request.getRefereeNote(), "refereeNote");
        RaceComplaint complaint = getPendingComplaintForReferee(id, referee.getRefereeId());
        complaint.setStatus(STATUS_REJECTED);
        complaint.setRefereeId(referee.getRefereeId());
        complaint.setRefereeNote(note);
        complaint.setResolvedAt(LocalDateTime.now());
        return toResponse(raceComplaintRepository.save(complaint));
    }

    @Transactional
    public RaceComplaintResponse refereeForward(Integer id, RaceComplaintRequest request, HttpServletRequest httpRequest) {
        Referee referee = getCurrentReferee(httpRequest);
        String note = requireNote(request == null ? null : request.getRefereeNote(), "refereeNote");
        RaceComplaint complaint = getPendingComplaintForReferee(id, referee.getRefereeId());
        complaint.setStatus(STATUS_FORWARDED);
        complaint.setRefereeId(referee.getRefereeId());
        complaint.setRefereeNote(note);
        return toResponse(raceComplaintRepository.save(complaint));
    }

    public List<RaceComplaintResponse> getOrganizerComplaints(HttpServletRequest request) {
        User organizer = currentUserService.getCurrentUser(request);
        requireRole(organizer, "Organizer");
        return raceComplaintRepository.findForwardedToOrganizer(organizer.getUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public RaceComplaintResponse organizerResolve(Integer id, RaceComplaintRequest request, HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        requireRole(organizer, "Organizer");
        RaceComplaint complaint = getForwardedComplaintForOrganizer(id, organizer.getUserId());
        complaint.setStatus(STATUS_RESOLVED);
        complaint.setOrganizerId(organizer.getUserId());
        complaint.setOrganizerNote(trimToNull(request == null ? null : request.getOrganizerNote()));
        complaint.setResolvedAt(LocalDateTime.now());
        return toResponse(raceComplaintRepository.save(complaint));
    }

    @Transactional
    public RaceComplaintResponse organizerReject(Integer id, RaceComplaintRequest request, HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        requireRole(organizer, "Organizer");
        String note = requireNote(request == null ? null : request.getOrganizerNote(), "organizerNote");
        RaceComplaint complaint = getForwardedComplaintForOrganizer(id, organizer.getUserId());
        complaint.setStatus(STATUS_REJECTED);
        complaint.setOrganizerId(organizer.getUserId());
        complaint.setOrganizerNote(note);
        complaint.setResolvedAt(LocalDateTime.now());
        return toResponse(raceComplaintRepository.save(complaint));
    }

    private RaceComplaint getPendingComplaintForReferee(Integer id, Integer refereeId) {
        RaceComplaint complaint = getComplaint(id);
        ensureRefereeAssigned(complaint.getRaceId(), refereeId);
        if (!STATUS_PENDING.equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalArgumentException("Only Pending complaints can be processed by referees.");
        }
        return complaint;
    }

    private RaceComplaint getForwardedComplaintForOrganizer(Integer id, Integer organizerUserId) {
        RaceComplaint complaint = getComplaint(id);
        ensureOrganizerOwnsRace(complaint.getRaceId(), organizerUserId);
        if (!STATUS_FORWARDED.equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalArgumentException("Only Forwarded complaints can be processed by organizers.");
        }
        return complaint;
    }

    private void ensureRaceCanBeComplained(Race race) {
        boolean published = raceResultRepository.findByRaceId(race.getRaceId()).stream()
                .anyMatch(result -> "Published".equalsIgnoreCase(result.getApprovalStatus()));
        if (!"Finished".equalsIgnoreCase(race.getStatus()) && !published) {
            throw new IllegalArgumentException("Race complaints can only be created after the race is finished or results are published.");
        }
    }

    private void ensureOwnerOwnsEntry(User ownerUser, RaceEntry entry) {
        HorseOwner owner = horseOwnerRepository.findByUserId(ownerUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a HorseOwner profile."));
        Horse horse = horseRepository.findById(entry.getHorseId())
                .orElseThrow(() -> new IllegalArgumentException("Horse was not found."));
        if (!owner.getOwnerId().equals(horse.getOwnerId())) {
            throw new IllegalArgumentException("You can only complain about races your horse participated in.");
        }
    }

    private void ensureRefereeAssigned(Integer raceId, Integer refereeId) {
        if (!raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, refereeId)) {
            throw new IllegalArgumentException("Referee has not been assigned to this race.");
        }
    }

    private void ensureOrganizerOwnsRace(Integer raceId, Integer organizerUserId) {
        Race race = getRace(raceId);
        Tournament tournament = tournamentRepository.findById(race.getTournamentId())
                .orElseThrow(() -> new IllegalArgumentException("Tournament was not found."));
        if (!organizerUserId.equals(tournament.getCreatedBy())) {
            throw new IllegalArgumentException("Race does not belong to the current organizer.");
        }
    }

    private Referee getCurrentReferee(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        requireRole(user, "Referee");
        return refereeRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a Referee profile."));
    }

    private Race getRace(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId is required.");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race was not found."));
    }

    private RaceEntry getEntryInRace(Integer raceId, Integer entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId is required.");
        }
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry was not found."));
        if (!raceId.equals(entry.getRaceId())) {
            throw new IllegalArgumentException("This entry does not belong to the selected race.");
        }
        return entry;
    }

    private RaceComplaint getComplaint(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("complaintId is invalid.");
        }
        return raceComplaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Race complaint was not found."));
    }

    private void requireRole(User user, String roleName) {
        if (user == null || user.getRole() == null || !roleName.equalsIgnoreCase(user.getRole().getRoleName())
                || !Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("You do not have permission to perform this action.");
        }
    }

    private String requireNote(String value, String fieldName) {
        String note = trimToNull(value);
        if (note == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return note;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RaceComplaintResponse toResponse(RaceComplaint complaint) {
        User owner = userRepository.findById(complaint.getOwnerUserId()).orElse(null);
        Race race = raceRepository.findById(complaint.getRaceId()).orElse(null);
        RaceEntry entry = raceEntryRepository.findById(complaint.getEntryId()).orElse(null);
        Horse horse = entry == null ? null : horseRepository.findById(entry.getHorseId()).orElse(null);
        Referee referee = complaint.getRefereeId() == null ? null : refereeRepository.findById(complaint.getRefereeId()).orElse(null);
        User refereeUser = referee == null ? null : userRepository.findById(referee.getUserId()).orElse(null);
        User organizer = complaint.getOrganizerId() == null ? null : userRepository.findById(complaint.getOrganizerId()).orElse(null);

        return new RaceComplaintResponse(
                complaint.getComplaintId(),
                complaint.getOwnerUserId(),
                owner == null ? null : owner.getUsername(),
                owner == null ? null : owner.getFullName(),
                complaint.getRaceId(),
                race == null ? null : race.getRaceName(),
                complaint.getEntryId(),
                horse == null ? null : horse.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                complaint.getReason(),
                complaint.getEvidenceUrl(),
                complaint.getStatus(),
                complaint.getRefereeId(),
                refereeUser == null ? null : refereeUser.getFullName(),
                complaint.getRefereeNote(),
                complaint.getOrganizerId(),
                organizer == null ? null : organizer.getFullName(),
                complaint.getOrganizerNote(),
                complaint.getCreatedAt(),
                complaint.getResolvedAt()
        );
    }
}
