package com.horseracing.repository;

import com.horseracing.entity.BetSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetSelectionRepository extends JpaRepository<BetSelection, Integer> {
    List<BetSelection> findByTicketIdOrderBySelectionIdAsc(Integer ticketId);

    List<BetSelection> findByTicketIdInOrderBySelectionIdAsc(List<Integer> ticketIds);
}
