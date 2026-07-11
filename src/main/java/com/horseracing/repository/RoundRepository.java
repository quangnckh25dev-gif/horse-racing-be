package com.horseracing.repository;

import com.horseracing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findByTournamentIdOrderByRoundOrderAsc(Integer tournamentId);

    boolean existsByTournamentIdAndRoundOrder(Integer tournamentId, Integer roundOrder);

    boolean existsByTournamentIdAndRoundOrderAndRoundIdNot(Integer tournamentId, Integer roundOrder, Integer roundId);
}
