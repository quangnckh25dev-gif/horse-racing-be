package com.horseracing.service;

import com.horseracing.dto.DashboardResponse;
import com.horseracing.dto.RaceSummaryResponse;
import com.horseracing.dto.SharedDashboardResponse;
import com.horseracing.entity.Race;
import com.horseracing.repository.HorseRepository;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.RaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    @PersistenceContext
    private EntityManager entityManager;

    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;
    private final JockeyRepository jockeyRepository;
    private final LeaderboardService leaderboardService;

    public DashboardService(RaceRepository raceRepository,
                            HorseRepository horseRepository,
                            JockeyRepository jockeyRepository,
                            LeaderboardService leaderboardService) {
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
        this.jockeyRepository = jockeyRepository;
        this.leaderboardService = leaderboardService;
    }

    public DashboardResponse getDashboard() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT TotalActiveUsers, PendingApprovals, OngoingTournaments,
                               UpcomingRaces, FinishedRaces, TotalHorses, TotalJockeys,
                               TotalBets, WonBets
                        FROM vw_SystemDashboard
                        """)
                .getSingleResult();

        return new DashboardResponse(
                toInteger(row[0]),
                toInteger(row[1]),
                toInteger(row[2]),
                toInteger(row[3]),
                toInteger(row[4]),
                toInteger(row[5]),
                toInteger(row[6]),
                toInteger(row[7]),
                toInteger(row[8])
        );
    }

    public SharedDashboardResponse getSharedDashboard() {
        List<RaceSummaryResponse> featuredRaces = raceRepository.findFeaturedDashboardRaces()
                .stream()
                .map(this::toRaceSummary)
                .toList();

        return new SharedDashboardResponse(
                toInteger(raceRepository.count()),
                toInteger(raceRepository.countByStatus("Scheduled")),
                toInteger(raceRepository.countByStatus("Ongoing")),
                toInteger(raceRepository.countByStatus("Finished")),
                toInteger(raceRepository.countByStatus("RegistrationOpen")),
                toInteger(horseRepository.count()),
                toInteger(jockeyRepository.count()),
                leaderboardService.getGlobalJockeyLeaderboard().stream().limit(5).toList(),
                leaderboardService.getGlobalHorseLeaderboard().stream().limit(5).toList(),
                featuredRaces
        );
    }

    private Integer toInteger(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
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
