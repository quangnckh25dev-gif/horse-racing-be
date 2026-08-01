package com.horseracing.repository;

import com.horseracing.entity.BetTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetTicketRepository extends JpaRepository<BetTicket, Integer> {
    List<BetTicket> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<BetTicket> findByRaceIdAndStatus(Integer raceId, String status);
}
