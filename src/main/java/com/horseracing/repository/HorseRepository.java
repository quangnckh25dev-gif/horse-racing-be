package com.horseracing.repository;

import com.horseracing.entity.Horse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HorseRepository extends JpaRepository<Horse, Integer> {
    List<Horse> findByOwnerId(Integer ownerId);

    List<Horse> findByOwnerIdAndIsActive(Integer ownerId, Boolean isActive);

    boolean existsByRegisterCode(String registerCode);

    @Query(value = """
            SELECT CASE WHEN COUNT(1) > 0 THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END
            FROM Horses
            WHERE OwnerID = :ownerId
              AND LOWER(LTRIM(RTRIM(HorseName))) = LOWER(LTRIM(RTRIM(:horseName)))
              AND (:horseId IS NULL OR HorseID <> :horseId)
            """, nativeQuery = true)
    boolean existsDuplicateNameForOwner(@Param("ownerId") Integer ownerId,
                                        @Param("horseName") String horseName,
                                        @Param("horseId") Integer horseId);

    @Query(value = """
            SELECT ranked.HorseRank
            FROM (
                SELECT
                    h.HorseID,
                    ROW_NUMBER() OVER (
                        ORDER BY
                            ISNULL(stats.TotalPoints, 0) DESC,
                            ISNULL(stats.Wins, 0) DESC,
                            h.HorseID ASC
                    ) AS HorseRank
                FROM Horses h
                LEFT JOIN HorseTournamentStats stats ON h.HorseID = stats.HorseID
                WHERE h.IsActive = 1
            ) ranked
            WHERE ranked.HorseID = :horseId
            """, nativeQuery = true)
    Optional<Integer> findHorseRank(@Param("horseId") Integer horseId);
}
