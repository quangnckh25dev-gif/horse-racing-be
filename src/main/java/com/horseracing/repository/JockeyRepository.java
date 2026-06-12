package com.horseracing.repository;

import com.horseracing.entity.Jockey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JockeyRepository extends JpaRepository<Jockey, Integer> {
    Optional<Jockey> findByUserId(Integer userId);

    List<Jockey> findByUserIdIn(List<Integer> userIds);
}
