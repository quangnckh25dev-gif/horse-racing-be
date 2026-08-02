package com.horseracing.repository;

import com.horseracing.entity.RaceComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaceComplaintRepository extends JpaRepository<RaceComplaint, Integer> {

    List<RaceComplaint> findByOwnerUserIdOrderByCreatedAtDesc(Integer ownerUserId);

    @Query(value = """
            SELECT COUNT(1)
            FROM RaceComplaints
            WHERE RaceID = :raceId
              AND Status IN ('Pending', 'Forwarded')
            """, nativeQuery = true)
    int countOpenComplaintsByRaceId(@Param("raceId") Integer raceId);

    @Query(value = """
            SELECT rc.*
            FROM RaceComplaints rc
            JOIN RaceReferees rr ON rr.RaceID = rc.RaceID
            WHERE rr.RefereeID = :refereeId
            ORDER BY rc.CreatedAt DESC
            """, nativeQuery = true)
    List<RaceComplaint> findAssignedToReferee(@Param("refereeId") Integer refereeId);

    @Query(value = """
            SELECT rc.*
            FROM RaceComplaints rc
            JOIN Races r ON r.RaceID = rc.RaceID
            JOIN Tournaments t ON t.TournamentID = r.TournamentID
            WHERE t.CreatedBy = :organizerUserId
              AND rc.Status = 'Forwarded'
            ORDER BY rc.CreatedAt DESC
            """, nativeQuery = true)
    List<RaceComplaint> findForwardedToOrganizer(@Param("organizerUserId") Integer organizerUserId);
}
