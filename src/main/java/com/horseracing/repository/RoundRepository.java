package com.horseracing.repository;

import com.horseracing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findByTournamentIdOrderByRoundOrderAsc(Integer tournamentId);

    Optional<Round> findByTournamentIdAndRoundOrder(Integer tournamentId, Integer roundOrder);

    boolean existsByTournamentIdAndRoundOrder(Integer tournamentId, Integer roundOrder);

    boolean existsByTournamentIdAndRoundOrderAndRoundIdNot(Integer tournamentId, Integer roundOrder, Integer roundId);
}
