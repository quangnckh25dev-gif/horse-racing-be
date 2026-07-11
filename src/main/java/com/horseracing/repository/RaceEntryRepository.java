package com.horseracing.repository;

import com.horseracing.entity.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaceEntryRepository extends JpaRepository<RaceEntry, Integer> {
    List<RaceEntry> findByRaceId(Integer raceId);

    @Query(value = """
            SELECT *
            FROM RaceEntries
            WHERE RaceID = :raceId
              AND RegistrationStatus IN ('Approved', 'Ready')
            ORDER BY
              CASE WHEN LaneNumber IS NULL THEN 1 ELSE 0 END,
              LaneNumber,
              EntryID
            """, nativeQuery = true)
    List<RaceEntry> findPublicEntriesByRaceId(@Param("raceId") Integer raceId);

    @Query(value = """
            SELECT re.*
            FROM RaceEntries re
            JOIN Horses h ON re.HorseID = h.HorseID
            WHERE h.OwnerID = :ownerId
            ORDER BY re.RegisteredAt DESC
            """, nativeQuery = true)
    List<RaceEntry> findByOwnerId(@Param("ownerId") Integer ownerId);

    @Query(value = """
            SELECT re.*
            FROM RaceEntries re
            JOIN Horses h ON re.HorseID = h.HorseID
            WHERE h.OwnerID = :ownerId
              AND re.RegistrationStatus = 'Approved'
            ORDER BY re.RegisteredAt DESC
            """, nativeQuery = true)
    List<RaceEntry> findApprovedByOwnerId(@Param("ownerId") Integer ownerId);

    long countByRaceIdAndRegistrationStatusNot(Integer raceId, String registrationStatus);

    boolean existsByRaceIdAndHorseIdAndRegistrationStatusNot(Integer raceId, Integer horseId, String registrationStatus);

    @Query(value = """
            SELECT COUNT(*)
            FROM RaceEntries
            WHERE RaceID = :raceId
              AND RegistrationStatus NOT IN ('Rejected', 'Withdrawn')
            """, nativeQuery = true)
    long countActiveRegistrations(@Param("raceId") Integer raceId);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END
            FROM RaceEntries
            WHERE RaceID = :raceId
              AND HorseID = :horseId
              AND RegistrationStatus NOT IN ('Rejected', 'Withdrawn')
            """, nativeQuery = true)
    boolean existsActiveRegistration(@Param("raceId") Integer raceId, @Param("horseId") Integer horseId);
}
