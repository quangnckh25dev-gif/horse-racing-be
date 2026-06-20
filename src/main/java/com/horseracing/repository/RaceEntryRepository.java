package com.horseracing.repository;

import com.horseracing.entity.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaceEntryRepository extends JpaRepository<RaceEntry, Integer> {
    List<RaceEntry> findByRaceId(Integer raceId);

    @Query(value = """
            SELECT re.*
            FROM RaceEntries re
            JOIN Horses h ON re.HorseID = h.HorseID
            WHERE h.OwnerID = :ownerId
            ORDER BY re.RegisteredAt DESC
            """, nativeQuery = true)
    List<RaceEntry> findByOwnerId(@Param("ownerId") Integer ownerId);

    long countByRaceIdAndRegistrationStatusNot(Integer raceId, String registrationStatus);

    boolean existsByRaceIdAndHorseIdAndRegistrationStatusNot(Integer raceId, Integer horseId, String registrationStatus);
}
