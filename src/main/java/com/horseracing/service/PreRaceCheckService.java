package com.horseracing.service;

import com.horseracing.dto.PreRaceCheckRequest;
import com.horseracing.dto.PreRaceCheckResponse;
import com.horseracing.entity.Horse;
import com.horseracing.entity.PreRaceCheck;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.Referee;
import com.horseracing.entity.User;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.PreRaceCheckRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RefereeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PreRaceCheckService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_CHECKED = "Checked";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String ENTRY_STATUS_PRE_RACE_REJECTED = "PreRaceRejected";
    private static final Set<String> CHECKABLE_ENTRY_STATUSES = Set.of("Approved", "Ready");

    private final PreRaceCheckRepository preRaceCheckRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final RefereeRepository refereeRepository;
    private final HorseRepository horseRepository;

    public PreRaceCheckService(PreRaceCheckRepository preRaceCheckRepository,
                               RaceRepository raceRepository,
                               RaceEntryRepository raceEntryRepository,
                               RaceRefereeRepository raceRefereeRepository,
                               RefereeRepository refereeRepository,
                               HorseRepository horseRepository) {
        this.preRaceCheckRepository = preRaceCheckRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.refereeRepository = refereeRepository;
        this.horseRepository = horseRepository;
    }

    public List<PreRaceCheckResponse> getChecks(Integer raceId, User currentUser) {
        requireAssignedReferee(raceId, currentUser);
        return preRaceCheckRepository.findByRaceIdOrderByEntryId(raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<PreRaceCheckResponse> initChecks(Integer raceId, User currentUser) {
        Referee referee = requireAssignedReferee(raceId, currentUser);
        Race race = getRaceOrThrow(raceId);
        if ("Draft".equalsIgnoreCase(race.getStatus())) {
            throw new IllegalArgumentException("Draft races cannot be initialized for pre-race checks.");
        }

        raceEntryRepository.findByRaceId(raceId)
                .stream()
                .filter(entry -> CHECKABLE_ENTRY_STATUSES.contains(entry.getRegistrationStatus()))
                .filter(entry -> !preRaceCheckRepository.existsByRaceIdAndEntryId(raceId, entry.getEntryId()))
                .forEach(entry -> {
                    PreRaceCheck check = new PreRaceCheck();
                    check.setRaceId(raceId);
                    check.setEntryId(entry.getEntryId());
                    check.setHorseId(entry.getHorseId());
                    check.setRefereeId(referee.getRefereeId());
                    check.setStatus(STATUS_PENDING);
                    preRaceCheckRepository.save(check);
                });

        return getChecks(raceId, currentUser);
    }

    @Transactional
    public PreRaceCheckResponse updateCheck(Integer raceId, Integer entryId, PreRaceCheckRequest request,
                                            User currentUser) {
        Referee referee = requireAssignedReferee(raceId, currentUser);
        RaceEntry entry = getEntryOrThrow(raceId, entryId);
        PreRaceCheck check = preRaceCheckRepository.findByRaceIdAndEntryId(raceId, entryId)
                .orElseGet(() -> createPendingCheck(raceId, entry, referee));

        String status = normalizeStatus(request == null ? null : request.getStatus());
        String reason = trimToNull(request == null ? null : request.getReason());
        if (STATUS_REJECTED.equals(status) && reason == null) {
            throw new IllegalArgumentException("Reject reason is required.");
        }

        check.setRefereeId(referee.getRefereeId());
        check.setStatus(status);
        check.setReason(STATUS_REJECTED.equals(status) ? reason : null);
        check.setCheckedAt(LocalDateTime.now());

        if (STATUS_REJECTED.equals(status)) {
            entry.setRegistrationStatus(ENTRY_STATUS_PRE_RACE_REJECTED);
            entry.setRejectReason(reason);
            raceEntryRepository.save(entry);
        }

        return toResponse(preRaceCheckRepository.save(check));
    }

    private Referee requireAssignedReferee(Integer raceId, User user) {
        if (user == null || user.getRole() == null
                || !"Referee".equalsIgnoreCase(user.getRole().getRoleName())
                || !Boolean.TRUE.equals(user.getIsActive())
                || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Only assigned referees can manage pre-race checks.");
        }
        getRaceOrThrow(raceId);
        Referee referee = refereeRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Current user does not have a Referee profile."));
        if (!raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, referee.getRefereeId())) {
            throw new IllegalArgumentException("Referee has not been assigned to this race.");
        }
        return referee;
    }

    private Race getRaceOrThrow(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId is invalid.");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Race was not found."));
    }

    private RaceEntry getEntryOrThrow(Integer raceId, Integer entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId is required.");
        }
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry was not found."));
        if (!raceId.equals(entry.getRaceId())) {
            throw new IllegalArgumentException("This entry does not belong to the selected race.");
        }
        if (!CHECKABLE_ENTRY_STATUSES.contains(entry.getRegistrationStatus())
                && !ENTRY_STATUS_PRE_RACE_REJECTED.equals(entry.getRegistrationStatus())) {
            throw new IllegalArgumentException("Only approved or ready entries can be checked.");
        }
        return entry;
    }

    private PreRaceCheck createPendingCheck(Integer raceId, RaceEntry entry, Referee referee) {
        PreRaceCheck check = new PreRaceCheck();
        check.setRaceId(raceId);
        check.setEntryId(entry.getEntryId());
        check.setHorseId(entry.getHorseId());
        check.setRefereeId(referee.getRefereeId());
        check.setStatus(STATUS_PENDING);
        return check;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required.");
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "checked", "check", "approved", "pass", "passed" -> STATUS_CHECKED;
            case "rejected", "reject", "failed", "fail" -> STATUS_REJECTED;
            case "pending" -> STATUS_PENDING;
            default -> throw new IllegalArgumentException("Pre-race check status only accepts Pending, Checked, or Rejected.");
        };
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PreRaceCheckResponse toResponse(PreRaceCheck check) {
        Horse horse = horseRepository.findById(check.getHorseId()).orElse(null);
        return new PreRaceCheckResponse(
                check.getPreRaceCheckId(),
                check.getRaceId(),
                check.getEntryId(),
                check.getHorseId(),
                horse == null ? null : horse.getHorseName(),
                check.getRefereeId(),
                check.getStatus(),
                check.getReason(),
                check.getCheckedAt()
        );
    }
}
