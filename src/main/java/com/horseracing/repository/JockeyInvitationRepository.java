package com.horseracing.repository;

import com.horseracing.entity.JockeyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JockeyInvitationRepository extends JpaRepository<JockeyInvitation, Integer> {
    List<JockeyInvitation> findByJockeyIdOrderByInvitedAtDesc(Integer jockeyId);

    List<JockeyInvitation> findByInvitedByOwnerOrderByInvitedAtDesc(Integer invitedByOwner);

    boolean existsByEntryIdAndJockeyIdAndStatus(Integer entryId, Integer jockeyId, String status);

    boolean existsByEntryIdAndStatus(Integer entryId, String status);
}
