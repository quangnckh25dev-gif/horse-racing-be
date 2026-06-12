package com.horseracing.repository;

import com.horseracing.entity.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceEntryRepository extends JpaRepository<RaceEntry, Integer> {
    List<RaceEntry> findByRaceId(Integer raceId);

    long countByRaceIdAndRegistrationStatusNot(Integer raceId, String registrationStatus);

    boolean existsByRaceIdAndHorseIdAndRegistrationStatusNot(Integer raceId, Integer horseId, String registrationStatus);
}