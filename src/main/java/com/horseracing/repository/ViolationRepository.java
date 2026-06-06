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

    @Query(value = "SELECT COUNT(1) FROM Referees WHERE RefereeID = :refereeId", nativeQuery = true)
    int countRefereeById(@Param("refereeId") Integer refereeId);
}
