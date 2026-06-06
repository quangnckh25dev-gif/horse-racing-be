package com.horseracing.repository;

import com.horseracing.entity.RaceMinute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RaceMinuteRepository extends JpaRepository<RaceMinute, Integer> {

    Optional<RaceMinute> findByRaceId(Integer raceId);

    boolean existsByRaceId(Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM Races WHERE RaceID = :raceId", nativeQuery = true)
    int countRaceById(@Param("raceId") Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM Referees WHERE RefereeID = :refereeId", nativeQuery = true)
    int countRefereeById(@Param("refereeId") Integer refereeId);
}
