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
