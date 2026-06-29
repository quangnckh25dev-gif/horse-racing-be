package com.horseracing.service;

import com.horseracing.dto.RaceSummaryResponse;
import com.horseracing.dto.RoundSummaryResponse;
import com.horseracing.dto.TournamentDetailResponse;
import com.horseracing.dto.TournamentRequest;
import com.horseracing.dto.TournamentResponse;
import com.horseracing.entity.Race;
import com.horseracing.entity.Round;
import com.horseracing.entity.Tournament;
import com.horseracing.repository.RaceRepository;
import com.horseracing.repository.RoundRepository;
import com.horseracing.repository.TournamentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TournamentService {

    private static final Set<String> VALID_STATUSES = Set.of("Draft", "Open", "Ongoing", "Finished", "Cancelled");

    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;
    private final RaceRepository raceRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             RoundRepository roundRepository,
                             RaceRepository raceRepository) {
        this.tournamentRepository = tournamentRepository;
        this.roundRepository = roundRepository;
        this.raceRepository = raceRepository;
    }

    public List<TournamentResponse> getAllTournaments() {
        return tournamentRepository.findAll()
                .stream()
                .map(this::toTournamentResponse)
                .toList();
    }

    public TournamentDetailResponse getTournamentDetail(Integer tournamentId) {
        Tournament tournament = getTournamentOrThrow(tournamentId);
        List<RoundSummaryResponse> rounds = roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId)
                .stream()
                .map(this::toRoundSummary)
                .toList();
        List<RaceSummaryResponse> races = raceRepository.findByTournamentId(tournamentId)
                .stream()
                .map(this::toRaceSummary)
                .toList();

        return toTournamentDetailResponse(tournament, rounds, races);
    }

    public List<RoundSummaryResponse> getTournamentRounds(Integer tournamentId) {
        getTournamentOrThrow(tournamentId);
        return roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId)
                .stream()
                .map(this::toRoundSummary)
                .toList();
    }

    public TournamentResponse createTournament(TournamentRequest request) {
        validateTournamentRequest(request);

        Tournament tournament = new Tournament();
        applyRequest(tournament, request);
        tournament.setStatus(resolveStatus(request.getStatus(), "Draft"));

        return toTournamentResponse(tournamentRepository.save(tournament));
    }

    public TournamentResponse updateTournament(Integer tournamentId, TournamentRequest request) {
        validateTournamentRequest(request);

        Tournament tournament = getTournamentOrThrow(tournamentId);
        applyRequest(tournament, request);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            tournament.setStatus(resolveStatus(request.getStatus(), tournament.getStatus()));
        }

        return toTournamentResponse(tournamentRepository.save(tournament));
    }

    public TournamentResponse updateStatus(Integer tournamentId, String status) {
        Tournament tournament = getTournamentOrThrow(tournamentId);
        tournament.setStatus(resolveStatus(status, tournament.getStatus()));
        return toTournamentResponse(tournamentRepository.save(tournament));
    }

    public void deleteTournament(Integer tournamentId) {
        Tournament tournament = getTournamentOrThrow(tournamentId);
        tournament.setStatus("Cancelled");
        tournamentRepository.save(tournament);
    }

    private Tournament getTournamentOrThrow(Integer tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("Tournament id khong hop le");
        }

        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tournament"));
    }

    private void validateTournamentRequest(TournamentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu tournament khong duoc de trong");
        }
        if (request.getTournamentName() == null || request.getTournamentName().isBlank()) {
            throw new IllegalArgumentException("TournamentName khong duoc de trong");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("StartDate va EndDate khong duoc de trong");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("EndDate phai lon hon hoac bang StartDate");
        }
        if (request.getPrizeFund() != null && request.getPrizeFund().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("PrizeFund khong duoc am");
        }
    }

    private void applyRequest(Tournament tournament, TournamentRequest request) {
        tournament.setTournamentName(request.getTournamentName());
        tournament.setDescription(request.getDescription());
        tournament.setLocation(request.getLocation());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setPrizeFund(request.getPrizeFund() == null ? BigDecimal.ZERO : request.getPrizeFund());
        tournament.setCreatedByAdmin(request.getCreatedByAdmin());
    }

    private String resolveStatus(String status, String defaultStatus) {
        String resolved = (status == null || status.isBlank()) ? defaultStatus : normalizeStatus(status);
        if (!VALID_STATUSES.contains(resolved)) {
            throw new IllegalArgumentException("Trạng thái giải đấu không hợp lệ. Chỉ chấp nhận: Nháp, Mở đăng ký, Đang diễn ra, Kết thúc, Đã hủy");
        }
        return resolved;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "draft", "nháp", "nhap" -> "Draft";
            case "open", "mở đăng ký", "mo dang ky", "registrationopen" -> "Open";
            case "ongoing", "đang diễn ra", "dang dien ra" -> "Ongoing";
            case "finished", "kết thúc", "ket thuc" -> "Finished";
            case "cancelled", "canceled", "đã hủy", "da huy" -> "Cancelled";
            default -> status.trim();
        };
    }

    private TournamentResponse toTournamentResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getTournamentId(),
                tournament.getTournamentName(),
                tournament.getDescription(),
                tournament.getLocation(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getPrizeFund(),
                tournament.getStatus(),
                tournament.getCreatedByAdmin(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt()
        );
    }

    private TournamentDetailResponse toTournamentDetailResponse(Tournament tournament,
                                                               List<RoundSummaryResponse> rounds,
                                                               List<RaceSummaryResponse> races) {
        return new TournamentDetailResponse(
                tournament.getTournamentId(),
                tournament.getTournamentName(),
                tournament.getDescription(),
                tournament.getLocation(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getPrizeFund(),
                tournament.getStatus(),
                tournament.getCreatedByAdmin(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt(),
                rounds,
                races
        );
    }

    private RoundSummaryResponse toRoundSummary(Round round) {
        return new RoundSummaryResponse(
                round.getRoundId(),
                round.getTournamentId(),
                round.getRoundName(),
                round.getRoundOrder(),
                round.getStartDate(),
                round.getEndDate(),
                round.getDescription(),
                round.getCreatedAt()
        );
    }

    private RaceSummaryResponse toRaceSummary(Race race) {
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
