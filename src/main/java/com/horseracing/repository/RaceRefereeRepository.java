package com.horseracing.repository;

import com.horseracing.entity.RaceReferee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaceRefereeRepository extends JpaRepository<RaceReferee, Integer> {

    List<RaceReferee> findByRaceId(Integer raceId);

    Optional<RaceReferee> findByRaceIdAndRefereeId(Integer raceId, Integer refereeId);

    boolean existsByRaceIdAndRefereeId(Integer raceId, Integer refereeId);

    boolean existsByRaceIdAndRoleIgnoreCase(Integer raceId, String role);

    void deleteByRaceIdAndRefereeId(Integer raceId, Integer refereeId);
}
