package com.horseracing.repository;

import com.horseracing.entity.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RaceResultRepository extends JpaRepository<RaceResult, Integer> {

    List<RaceResult> findByRaceId(Integer raceId);

    Optional<RaceResult> findByRaceIdAndEntryId(Integer raceId, Integer entryId);

    @Query(value = "SELECT COUNT(1) FROM Races WHERE RaceID = :raceId", nativeQuery = true)
    int countRaceById(@Param("raceId") Integer raceId);

    @Query(value = "SELECT COUNT(1) FROM RaceEntries WHERE RaceID = :raceId AND EntryID = :entryId", nativeQuery = true)
    int countEntryInRace(@Param("raceId") Integer raceId, @Param("entryId") Integer entryId);

    @Query(value = "SELECT COUNT(1) FROM Users u JOIN Roles r ON u.RoleID = r.RoleID "
            + "WHERE u.UserID = :organizerId AND u.IsActive = 1 AND u.IsApproved = 1 "
            + "AND r.RoleName IN ('OrganizerHead', 'OrganizerMember')", nativeQuery = true)
    int countOrganizerById(@Param("organizerId") Integer organizerId);
}
