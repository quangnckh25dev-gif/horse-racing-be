package com.horseracing.repository;

import com.horseracing.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Integer> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
