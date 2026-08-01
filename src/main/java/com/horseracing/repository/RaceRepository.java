package com.horseracing.repository;

import com.horseracing.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaceRepository extends JpaRepository<Race, Integer> {

    List<Race> findByTournamentId(Integer tournamentId);

    List<Race> findByRoundId(Integer roundId);

    List<Race> findByStatus(String status);

    List<Race> findByTournamentIdAndRoundId(Integer tournamentId, Integer roundId);

    List<Race> findByTournamentIdAndStatus(Integer tournamentId, String status);

    List<Race> findByRoundIdAndStatus(Integer roundId, String status);

    List<Race> findByTournamentIdAndRoundIdAndStatus(Integer tournamentId, Integer roundId, String status);

    boolean existsByRoundId(Integer roundId);

    boolean existsByRoundIdAndRaceIdNot(Integer roundId, Integer raceId);

    long countByStatus(String status);

    @Query(value = """
            SELECT r.*
            FROM Races r
            JOIN Tournaments t ON t.TournamentID = r.TournamentID
            WHERE t.Status IN ('Open', 'Ongoing', 'Finished', 'Cancelled')
              AND (:tournamentId IS NULL OR r.TournamentID = :tournamentId)
              AND (:roundId IS NULL OR r.RoundID = :roundId)
              AND (:status IS NULL OR r.Status = :status)
            ORDER BY r.RaceDate ASC
            """, nativeQuery = true)
    List<Race> findPublicRaces(@Param("tournamentId") Integer tournamentId,
                               @Param("roundId") Integer roundId,
                               @Param("status") String status);

    @Query(value = """
            SELECT TOP 5 r.*
            FROM Races r
            ORDER BY
                CASE r.Status
                    WHEN 'Ongoing' THEN 1
                    WHEN 'RegistrationOpen' THEN 2
                    WHEN 'Draft' THEN 3
                    WHEN 'Finished' THEN 4
                    ELSE 5
                END,
                r.RaceDate ASC
            """, nativeQuery = true)
    List<Race> findFeaturedDashboardRaces();

    @Query(value = """
            SELECT r.*
            FROM Races r
            JOIN Tournaments t ON t.TournamentID = r.TournamentID
            WHERE t.CreatedBy = :organizerUserId
            ORDER BY r.RaceDate ASC
            """, nativeQuery = true)
    List<Race> findByOrganizerUserId(@Param("organizerUserId") Integer organizerUserId);

    @Query(value = """
            SELECT r.*
            FROM Races r
            JOIN RaceReferees rr ON rr.RaceID = r.RaceID
            JOIN Referees ref ON ref.RefereeID = rr.RefereeID
            WHERE ref.UserID = :userId
            ORDER BY r.RaceDate ASC
            """, nativeQuery = true)
    List<Race> findAssignedRacesByRefereeUserId(@Param("userId") Integer userId);

    @Query(value = """
            SELECT COUNT(1)
            FROM RaceReferees rr
            JOIN Referees ref ON rr.RefereeID = ref.RefereeID
            WHERE rr.RaceID = :raceId AND ref.UserID = :userId
            """, nativeQuery = true)
    int countAssignedReferee(@Param("raceId") Integer raceId, @Param("userId") Integer userId);

    @Modifying
    @Query(value = """
            INSERT INTO RaceStatusHistory (RaceID, OldStatus, NewStatus, ChangedBy)
            VALUES (:raceId, :oldStatus, :newStatus, :changedBy)
            """, nativeQuery = true)
    void insertStatusHistory(@Param("raceId") Integer raceId,
                             @Param("oldStatus") String oldStatus,
                             @Param("newStatus") String newStatus,
                             @Param("changedBy") Integer changedBy);
}
