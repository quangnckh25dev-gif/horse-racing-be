package com.horseracing.service;

import com.horseracing.dto.RaceRequest;
import com.horseracing.dto.RaceSummaryResponse;
import com.horseracing.entity.Race;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class RaceService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "Scheduled", "RegistrationOpen", "Ongoing", "Finished", "Cancelled"
    );

    private final RaceRepository raceRepository;
    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;

    public RaceService(RaceRepository raceRepository,
                       TournamentRepository tournamentRepository,
                       RoundRepository roundRepository) {
        this.raceRepository = raceRepository;
        this.tournamentRepository = tournamentRepository;
        this.roundRepository = roundRepository;
    }

    public List<RaceSummaryResponse> getRaces(Integer tournamentId, Integer roundId, String status) {
        if (tournamentId != null) {
            ensureTournamentExists(tournamentId);
        }
        if (roundId != null) {
            ensureRoundExists(roundId);
        }
        if (status != null && !status.isBlank()) {
            resolveStatus(status, status);
        }

        List<Race> races;
        if (tournamentId != null && roundId != null && status != null && !status.isBlank()) {
            races = raceRepository.findByTournamentIdAndRoundIdAndStatus(tournamentId, roundId, status);
        } else if (tournamentId != null && roundId != null) {
            races = raceRepository.findByTournamentIdAndRoundId(tournamentId, roundId);
        } else if (tournamentId != null && status != null && !status.isBlank()) {
            races = raceRepository.findByTournamentIdAndStatus(tournamentId, status);
        } else if (roundId != null && status != null && !status.isBlank()) {
            races = raceRepository.findByRoundIdAndStatus(roundId, status);
        } else if (tournamentId != null) {
            races = raceRepository.findByTournamentId(tournamentId);
        } else if (roundId != null) {
            races = raceRepository.findByRoundId(roundId);
        } else if (status != null && !status.isBlank()) {
            races = raceRepository.findByStatus(status);
        } else {
            races = raceRepository.findAll();
        }

        return races.stream().map(this::toResponse).toList();
    }

    public RaceSummaryResponse getRaceDetail(Integer raceId) {
        return toResponse(getRaceOrThrow(raceId));
    }

    public RaceSummaryResponse createRace(RaceRequest request) {
        validateRaceRequest(request);

        Race race = new Race();
        applyRequest(race, request);
        race.setStatus(resolveStatus(request.getStatus(), "Scheduled"));

        return toResponse(raceRepository.save(race));
    }

    public RaceSummaryResponse updateRace(Integer raceId, RaceRequest request) {
        validateRaceRequest(request);

        Race race = getRaceOrThrow(raceId);
        applyRequest(race, request);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            race.setStatus(resolveStatus(request.getStatus(), race.getStatus()));
        }

        return toResponse(raceRepository.save(race));
    }

    public RaceSummaryResponse updateStatus(Integer raceId, String status) {
        Race race = getRaceOrThrow(raceId);
        race.setStatus(resolveStatus(status, race.getStatus()));
        return toResponse(raceRepository.save(race));
    }

    public void deleteRace(Integer raceId) {
        Race race = getRaceOrThrow(raceId);
        race.setStatus("Cancelled");
        raceRepository.save(race);
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

    private void validateRaceRequest(RaceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu race khong duoc de trong");
        }
        if (request.getRaceName() == null || request.getRaceName().isBlank()) {
            throw new IllegalArgumentException("RaceName khong duoc de trong");
        }
        if (request.getRaceDate() == null) {
            throw new IllegalArgumentException("RaceDate khong duoc de trong");
        }
        Tournament tournament = ensureTournamentExists(request.getTournamentId());
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
        String resolved = (status == null || status.isBlank()) ? defaultStatus : status;
        if (!VALID_STATUSES.contains(resolved)) {
            throw new IllegalArgumentException("Status khong hop le. Chi chap nhan: " + VALID_STATUSES);
        }
        return resolved;
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
