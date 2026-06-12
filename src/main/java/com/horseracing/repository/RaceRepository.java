package com.horseracing.repository;

import com.horseracing.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
