package com.horseracing.repository;

import com.horseracing.entity.RaceMinute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RaceMinuteRepository extends JpaRepository<RaceMinute, Integer> {

    Optional<RaceMinute> findByRaceId(Integer raceId);

    boolean existsByRaceId(Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM Races WHERE RaceID = :raceId", nativeQuery = true)
    int countRaceById(@Param("raceId") Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM RaceResults WHERE RaceID = :raceId", nativeQuery = true)
    int countResultsByRaceId(@Param("raceId") Integer raceId);

    @Query(value = "SELECT Status FROM Races WHERE RaceID = :raceId", nativeQuery = true)
    String findRaceStatusByRaceId(@Param("raceId") Integer raceId);

    @Query(value = """
            SELECT t.CreatedBy
            FROM Races r
            JOIN Tournaments t ON r.TournamentID = t.TournamentID
            WHERE r.RaceID = :raceId
            """, nativeQuery = true)
    Integer findOrganizerUserIdByRaceId(@Param("raceId") Integer raceId);

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

    @Modifying
    @Query(value = "EXEC sp_SendMinutesToOwners :raceId", nativeQuery = true)
    void sendMinutesToOwners(@Param("raceId") Integer raceId);
}
