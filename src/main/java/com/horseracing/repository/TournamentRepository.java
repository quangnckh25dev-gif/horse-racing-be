package com.horseracing.repository;

import com.horseracing.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Integer> {

    List<Tournament> findByStatus(String status);

    List<Tournament> findByCreatedByOrderByCreatedAtDesc(Integer createdBy);

    Optional<Tournament> findByTournamentIdAndCreatedBy(Integer tournamentId, Integer createdBy);
}
