package com.horseracing.repository;

import com.horseracing.entity.HorseHealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorseHealthRecordRepository extends JpaRepository<HorseHealthRecord, Integer> {
    List<HorseHealthRecord> findByHorseIdOrderByCheckDateDesc(Integer horseId);
}
