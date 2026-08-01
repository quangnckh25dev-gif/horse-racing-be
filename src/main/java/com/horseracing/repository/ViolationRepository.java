package com.horseracing.repository;

import com.horseracing.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Integer> {

    List<Violation> findByRaceId(Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM Races WHERE RaceID = :raceId", nativeQuery = true)
    int countRaceById(@Param("raceId") Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM RaceEntries WHERE RaceID = :raceId AND EntryID = :entryId", nativeQuery = true)
    int countEntryInRace(@Param("raceId") Integer raceId, @Param("entryId") Integer entryId);

    @Query(value = """
            SELECT COUNT(1)
            FROM RaceEntries
            WHERE RaceID = :raceId
              AND EntryID = :entryId
              AND RegistrationStatus IN ('Approved', 'Ready')
            """, nativeQuery = true)
    int countEligibleEntryInRace(@Param("raceId") Integer raceId, @Param("entryId") Integer entryId);

    @Query(value = "SELECT COUNT(1) FROM Referees WHERE RefereeID = :refereeId", nativeQuery = true)
    int countRefereeById(@Param("refereeId") Integer refereeId);

    @Query(value = """
            SELECT COUNT(1)
            FROM RaceReferees rr
            JOIN Referees ref ON rr.RefereeID = ref.RefereeID
            WHERE rr.RaceID = :raceId AND ref.UserID = :userId
            """, nativeQuery = true)
    int countAssignedReferee(@Param("raceId") Integer raceId, @Param("userId") Integer userId);

    @Query(value = "SELECT RefereeID FROM Referees WHERE UserID = :userId", nativeQuery = true)
    Integer findRefereeIdByUserId(@Param("userId") Integer userId);

    @Query(value = """
            SELECT COALESCE(SUM(PenaltySeconds), 0)
            FROM Violations
            WHERE RaceID = :raceId AND EntryID = :entryId
            """, nativeQuery = true)
    java.math.BigDecimal sumPenaltySecondsByRaceAndEntry(@Param("raceId") Integer raceId,
                                                         @Param("entryId") Integer entryId);

    @Query(value = """
            SELECT COUNT(1)
            FROM Violations
            WHERE RaceID = :raceId AND EntryID = :entryId AND IsDQ = 1
            """, nativeQuery = true)
    int countDqByRaceAndEntry(@Param("raceId") Integer raceId, @Param("entryId") Integer entryId);

    @Query(value = """
            SELECT COUNT(1)
            FROM Violations
            WHERE RaceID = :raceId AND EntryID = :entryId
            """, nativeQuery = true)
    int countEntryViolations(@Param("raceId") Integer raceId, @Param("entryId") Integer entryId);
}
