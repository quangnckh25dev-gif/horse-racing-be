package com.horseracing.repository;

import com.horseracing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findByTournamentIdOrderByRoundOrderAsc(Integer tournamentId);
}
