package com.horseracing.repository;

import com.horseracing.entity.RaceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceStatusHistoryRepository extends JpaRepository<RaceStatusHistory, Integer> {
}
