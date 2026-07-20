package com.horseracing.service;

import com.horseracing.dto.RaceRequest;
import com.horseracing.dto.RaceSummaryResponse;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceStatusHistory;
import com.horseracing.entity.Referee;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.entity.User;
import com.horseracing.repository.RaceRefereeRepository;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RaceStatusHistoryRepository;
import com.horseracing.repository.RefereeRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RaceService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "Scheduled", "RegistrationOpen", "Ongoing", "Finished", "Cancelled"
    );

    private final RaceRepository raceRepository;
    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;
    private final RefereeRepository refereeRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final RaceStatusHistoryRepository statusHistoryRepository;

    public RaceService(RaceRepository raceRepository,
                       TournamentRepository tournamentRepository,
                       RoundRepository roundRepository,
                       RefereeRepository refereeRepository,
                       RaceRefereeRepository raceRefereeRepository,
                       RaceStatusHistoryRepository statusHistoryRepository) {
        this.raceRepository = raceRepository;
        this.tournamentRepository = tournamentRepository;
        this.roundRepository = roundRepository;
        this.refereeRepository = refereeRepository;
        this.raceRefereeRepository = raceRefereeRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public List<RaceSummaryResponse> getRaces(Integer tournamentId, Integer roundId, String status) {
        if (tournamentId != null) {
            ensureTournamentExists(tournamentId);
        }
        if (roundId != null) {
            ensureRoundExists(roundId);
        }
        if (status != null && !status.isBlank()) {
            status = resolveStatus(status, status);
        }

        return raceRepository.findPublicRaces(tournamentId, roundId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RaceSummaryResponse> getOrganizerRaces(User organizer) {
        requireRole(organizer, "Organizer");
        return raceRepository.findByOrganizerUserId(organizer.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceSummaryResponse getRaceDetail(Integer raceId) {
        return toResponse(getRaceOrThrow(raceId));
    }

    public List<RaceSummaryResponse> getAssignedRacesForReferee(User refereeUser) {
        requireRole(refereeUser, "Referee");
        return raceRepository.findAssignedRacesByRefereeUserId(refereeUser.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    //của buiquangann
    public RaceSummaryResponse createRace(RaceRequest request, User organizer) {
        validateRaceRequest(request, organizer);

        Race race = new Race();
        applyRequest(race, request);
        race.setStatus("Scheduled");

        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceSummaryResponse updateRace(Integer raceId, RaceRequest request, User organizer) {
        validateRaceRequest(request, organizer);

        Race race = getRaceOrThrow(raceId);
        ensureOwnedTournament(race.getTournamentId(), organizer);
        if (!Set.of("Scheduled", "RegistrationOpen").contains(race.getStatus())) {
            throw new IllegalArgumentException("Khong the sua race da bat dau hoac ket thuc");
        }
        applyRequest(race, request);

        return toResponse(raceRepository.save(race));
    }

    @Transactional
    //của buiquangann
    public RaceSummaryResponse updateStatus(Integer raceId, String status, User refereeUser) {
        requireRole(refereeUser, "Referee");
        Race race = getRaceOrThrow(raceId);
        Referee referee = refereeRepository.findByUserId(refereeUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User chua co ho so Referee"));
        if (!raceRefereeRepository.existsByRaceIdAndRefereeId(raceId, referee.getRefereeId())) {
            throw new IllegalArgumentException("Referee chua duoc phan cong vao race nay");
        }

        String newStatus = resolveStatus(status, race.getStatus());
        if (race.getStatus() != null && race.getStatus().equals(newStatus)) {
            return toResponse(race);
        }
        validateTransition(race.getStatus(), newStatus);

        RaceStatusHistory history = new RaceStatusHistory();
        history.setRaceId(raceId);
        history.setOldStatus(race.getStatus());
        history.setNewStatus(newStatus);
        history.setChangedBy(refereeUser.getUserId());

        race.setStatus(newStatus);
        statusHistoryRepository.save(history);
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceSummaryResponse updateStatusByReferee(Integer raceId, String status, User currentUser) {
        return updateStatus(raceId, status, currentUser);
    }

    @Transactional
    public void deleteRace(Integer raceId, User organizer) {
        Race race = getRaceOrThrow(raceId);
        Tournament tournament = ensureOwnedTournament(race.getTournamentId(), organizer);
        if (!"Draft".equals(tournament.getStatus()) || !"Scheduled".equals(race.getStatus())) {
            throw new IllegalArgumentException("Chi duoc xoa race Scheduled khi tournament con Draft");
        }
        raceRepository.delete(race);
    }

    public RaceSummaryResponse getSchedule(Integer raceId) {
        return getRaceDetail(raceId);
    }

    private Race getRaceOrThrow(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("Race id khong hop le");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race"));
    }

    private Tournament ensureTournamentExists(Integer tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("TournamentID khong duoc de trong");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament"));
    }

    private Round ensureRoundExists(Integer roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay round"));
    }

    private void validateRaceRequest(RaceRequest request, User organizer) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu race khong duoc de trong");
        }
        if (request.getRaceName() == null || request.getRaceName().isBlank()) {
            throw new IllegalArgumentException("RaceName khong duoc de trong");
        }
        if (request.getRaceDate() == null) {
            throw new IllegalArgumentException("RaceDate khong duoc de trong");
        }
        Tournament tournament = ensureOwnedTournament(request.getTournamentId(), organizer);
        if (request.getRoundId() != null) {
            Round round = ensureRoundExists(request.getRoundId());
            if (!round.getTournamentId().equals(request.getTournamentId())) {
                throw new IllegalArgumentException("Round khong thuoc tournament nay");
            }
        }
        if (request.getRaceDate().toLocalDate().isBefore(tournament.getStartDate())
                || request.getRaceDate().toLocalDate().isAfter(tournament.getEndDate())) {
            throw new IllegalArgumentException("RaceDate phai nam trong thoi gian tournament");
        }
        if (request.getMaxParticipants() != null && request.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("MaxParticipants phai lon hon 0");
        }
        if (request.getTrackLength() != null && request.getTrackLength() <= 0) {
            throw new IllegalArgumentException("TrackLength phai lon hon 0");
        }
        validateNonNegative(request.getPrizeFirst(), "PrizeFirst");
        validateNonNegative(request.getPrizeSecond(), "PrizeSecond");
        validateNonNegative(request.getPrizeThird(), "PrizeThird");
        validateNonNegative(request.getPrizePool(), "PrizePool");
        if (request.getRegistrationOpen() != null && request.getRegistrationClose() != null
                && request.getRegistrationClose().isBefore(request.getRegistrationOpen())) {
            throw new IllegalArgumentException("RegistrationClose phai sau RegistrationOpen");
        }
        if (request.getRegistrationClose() != null && request.getRegistrationClose().isAfter(request.getRaceDate())) {
            throw new IllegalArgumentException("RegistrationClose khong duoc sau RaceDate");
        }
    }

    private void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " khong duoc am");
        }
    }

    private void applyRequest(Race race, RaceRequest request) {
        race.setTournamentId(request.getTournamentId());
        race.setRoundId(request.getRoundId());
        race.setRaceName(request.getRaceName());
        race.setRaceDate(request.getRaceDate());
        race.setTrackLength(request.getTrackLength());
        race.setTrackType(request.getTrackType());
        race.setMaxParticipants(request.getMaxParticipants());
        if (request.getPrizePool() != null
                && request.getPrizeFirst() == null
                && request.getPrizeSecond() == null
                && request.getPrizeThird() == null) {
            race.setPrizeFirst(defaultMoney(request.getPrizePool()));
            race.setPrizeSecond(BigDecimal.ZERO);
            race.setPrizeThird(BigDecimal.ZERO);
        } else {
            race.setPrizeFirst(defaultMoney(request.getPrizeFirst()));
            race.setPrizeSecond(defaultMoney(request.getPrizeSecond()));
            race.setPrizeThird(defaultMoney(request.getPrizeThird()));
        }
        race.setRegistrationOpen(request.getRegistrationOpen());
        race.setRegistrationClose(request.getRegistrationClose());
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String resolveStatus(String status, String defaultStatus) {
        String resolved = (status == null || status.isBlank()) ? defaultStatus : normalizeStatus(status);
        if (!VALID_STATUSES.contains(resolved)) {
            throw new IllegalArgumentException("Trang thai vong dua khong hop le");
        }
        return resolved;
    }

    private Tournament ensureOwnedTournament(Integer tournamentId, User organizer) {
        requireRole(organizer, "Organizer");
        return tournamentRepository.findByTournamentIdAndCreatedBy(tournamentId, organizer.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Tournament khong thuoc Organizer hien tai"));
    }

    private void requireRole(User user, String role) {
        if (user == null || user.getRole() == null || !role.equalsIgnoreCase(user.getRole().getRoleName())
                || !Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Ban khong co quyen thuc hien thao tac nay");
        }
    }

    private void validateTransition(String oldStatus, String newStatus) {
        boolean valid = ("Scheduled".equals(oldStatus) && Set.of("RegistrationOpen", "Ongoing", "Cancelled").contains(newStatus))
                || ("RegistrationOpen".equals(oldStatus) && Set.of("Ongoing", "Cancelled").contains(newStatus))
                || ("Ongoing".equals(oldStatus) && Set.of("Finished", "Cancelled").contains(newStatus));
        if (!valid) {
            throw new IllegalArgumentException("Chuyen trang thai race khong hop le");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "scheduled", "da len lich" -> "Scheduled";
            case "registrationopen", "registration open", "mo dang ky" -> "RegistrationOpen";
            case "ongoing", "dang dien ra" -> "Ongoing";
            case "finished", "ket thuc" -> "Finished";
            case "cancelled", "canceled", "da huy" -> "Cancelled";
            default -> status.trim();
        };
    }

    private RaceSummaryResponse toResponse(Race race) {
        return new RaceSummaryResponse(
                race.getRaceId(),
                race.getTournamentId(),
                race.getRoundId(),
                race.getRaceName(),
                race.getRaceDate(),
                race.getTrackLength(),
                race.getTrackType(),
                race.getMaxParticipants(),
                race.getPrizeFirst(),
                race.getPrizeSecond(),
                race.getPrizeThird(),
                race.getStatus(),
                race.getRegistrationOpen(),
                race.getRegistrationClose()
        );
    }
}
