package com.horseracing.repository;

import com.horseracing.entity.PreRaceCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreRaceCheckRepository extends JpaRepository<PreRaceCheck, Integer> {
    List<PreRaceCheck> findByRaceIdOrderByEntryId(Integer raceId);

    Optional<PreRaceCheck> findByRaceIdAndEntryId(Integer raceId, Integer entryId);

    boolean existsByRaceIdAndEntryId(Integer raceId, Integer entryId);
}
