package com.horseracing.repository;

import com.horseracing.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Integer> {
    Optional<Prediction> findByUserIdAndRaceId(Integer userId, Integer raceId);

    List<Prediction> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Prediction> findByRaceId(Integer raceId);
}
