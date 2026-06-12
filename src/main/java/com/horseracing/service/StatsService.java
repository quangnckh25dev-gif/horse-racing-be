package com.horseracing.service;

import com.horseracing.dto.IndividualStatsResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StatsService {

    @PersistenceContext
    private EntityManager entityManager;

    public IndividualStatsResponse getJockeyStats(Integer jockeyId) {
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
                        WHERE j.JockeyID = :jockeyId
                        GROUP BY j.JockeyID, u.FullName
                        """)
                .setParameter("jockeyId", jockeyId)
                .getResultList();

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay jockey");
        }

        return toResponse(rows.get(0));
    }

    public IndividualStatsResponse getHorseStats(Integer horseId) {
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
                        WHERE h.HorseID = :horseId
                        GROUP BY h.HorseID, h.HorseName
                        """)
                .setParameter("horseId", horseId)
                .getResultList();

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay horse");
        }

        return toResponse(rows.get(0));
    }

    private IndividualStatsResponse toResponse(Object[] row) {
        return new IndividualStatsResponse(
                toInteger(row[0]),
                row[1] == null ? null : row[1].toString(),
                toInteger(row[2]),
                toInteger(row[3]),
                toInteger(row[4]),
                toBigDecimal(row[5]),
                toInteger(row[6])
        );
    }

    private Integer toInteger(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
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
