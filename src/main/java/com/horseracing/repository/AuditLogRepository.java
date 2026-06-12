package com.horseracing.repository;

import com.horseracing.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
