package com.horseracing.repository;

import com.horseracing.entity.UserRoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleHistoryRepository extends JpaRepository<UserRoleHistory, Integer> {
}
