package com.horseracing.repository;

import com.horseracing.entity.Bet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, Integer> {
    List<Bet> findByUserIdAndRaceIdOrderByCreatedAtDesc(Integer userId, Integer raceId);

    List<Bet> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Bet> findByRaceIdAndStatus(Integer raceId, String status);
}
