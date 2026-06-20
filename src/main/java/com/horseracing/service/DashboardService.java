package com.horseracing.service;

import com.horseracing.dto.DashboardResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @PersistenceContext
    private EntityManager entityManager;

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

    private Integer toInteger(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }
}
