package com.horseracing.service;

import com.horseracing.dto.LeaderboardResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<LeaderboardResponse> getTournamentJockeyLeaderboard(Integer tournamentId) {
        ensureTournamentExists(tournamentId);

        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    j.JockeyID,
                    u.FullName,
                    COALESCE(jts.TotalRaces, 0),
                    COALESCE(jts.TotalWins, 0),
                    COALESCE(jts.TotalPodiums, 0),
                    COALESCE(jts.TotalPrize, 0),
                    COALESCE(jts.Points, 0)
                FROM JockeyTournamentStats jts
                JOIN Jockeys j ON jts.JockeyID = j.JockeyID
                JOIN Users u ON j.UserID = u.UserID
                WHERE jts.TournamentID = :tournamentId
                ORDER BY jts.Points DESC, jts.TotalWins DESC, jts.TotalPrize DESC
                """)
                .setParameter("tournamentId", tournamentId)
                .getResultList();

        return toLeaderboard(rows);
    }

    public List<LeaderboardResponse> getTournamentHorseLeaderboard(Integer tournamentId) {
        ensureTournamentExists(tournamentId);

        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    h.HorseID,
                    h.HorseName,
                    COALESCE(hts.TotalRaces, 0),
                    COALESCE(hts.TotalWins, 0),
                    COALESCE(hts.TotalPodiums, 0),
                    COALESCE(hts.TotalPrize, 0),
                    COALESCE(hts.Points, 0)
                FROM HorseTournamentStats hts
                JOIN Horses h ON hts.HorseID = h.HorseID
                WHERE hts.TournamentID = :tournamentId
                ORDER BY hts.Points DESC, hts.TotalWins DESC, hts.TotalPrize DESC
                """)
                .setParameter("tournamentId", tournamentId)
                .getResultList();

        return toLeaderboard(rows);
    }

    public List<LeaderboardResponse> getGlobalJockeyLeaderboard() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    j.JockeyID,
                    u.FullName,
                    COALESCE(SUM(jts.TotalRaces), 0),
                    COALESCE(SUM(jts.TotalWins), 0),
                    COALESCE(SUM(jts.TotalPodiums), 0),
                    COALESCE(SUM(jts.TotalPrize), 0),
                    COALESCE(SUM(jts.Points), 0)
                FROM Jockeys j
                JOIN Users u ON j.UserID = u.UserID
                LEFT JOIN JockeyTournamentStats jts ON j.JockeyID = jts.JockeyID
                GROUP BY j.JockeyID, u.FullName
                ORDER BY COALESCE(SUM(jts.Points), 0) DESC,
                         COALESCE(SUM(jts.TotalWins), 0) DESC,
                         COALESCE(SUM(jts.TotalPrize), 0) DESC
                """)
                .getResultList();

        return toLeaderboard(rows);
    }

    public List<LeaderboardResponse> getGlobalHorseLeaderboard() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    h.HorseID,
                    h.HorseName,
                    COALESCE(SUM(hts.TotalRaces), 0),
                    COALESCE(SUM(hts.TotalWins), 0),
                    COALESCE(SUM(hts.TotalPodiums), 0),
                    COALESCE(SUM(hts.TotalPrize), 0),
                    COALESCE(SUM(hts.Points), 0)
                FROM Horses h
                LEFT JOIN HorseTournamentStats hts ON h.HorseID = hts.HorseID
                GROUP BY h.HorseID, h.HorseName
                ORDER BY COALESCE(SUM(hts.Points), 0) DESC,
                         COALESCE(SUM(hts.TotalWins), 0) DESC,
                         COALESCE(SUM(hts.TotalPrize), 0) DESC
                """)
                .getResultList();

        return toLeaderboard(rows);
    }

    private void ensureTournamentExists(Integer tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("Tournament was not found.");
        }

        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(1) FROM Tournaments WHERE TournamentID = :tournamentId")
                .setParameter("tournamentId", tournamentId)
                .getSingleResult();

        if (count.intValue() == 0) {
            throw new IllegalArgumentException("Tournament was not found.");
        }
    }

    private List<LeaderboardResponse> toLeaderboard(List<Object[]> rows) {
        List<LeaderboardResponse> result = new ArrayList<>();
        int rank = 1;

        for (Object[] row : rows) {
            result.add(new LeaderboardResponse(
                    rank++,
                    toInteger(row[0]),
                    row[1] == null ? null : row[1].toString(),
                    toInteger(row[2]),
                    toInteger(row[3]),
                    toInteger(row[4]),
                    toBigDecimal(row[5]),
                    toInteger(row[6])
            ));
        }

        return result;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }

        return ((Number) value).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(value.toString());
    }
}
