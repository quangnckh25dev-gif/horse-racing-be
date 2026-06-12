package com.horseracing.repository;

import com.horseracing.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefereeRepository extends JpaRepository<Referee, Integer> {

    Optional<Referee> findByUserId(Integer userId);
}
