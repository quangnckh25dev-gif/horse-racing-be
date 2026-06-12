package com.horseracing.repository;

import com.horseracing.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Integer> {

    List<Tournament> findByStatus(String status);
}
