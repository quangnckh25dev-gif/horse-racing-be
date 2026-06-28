package com.horseracing.repository;

import com.horseracing.entity.Horse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorseRepository extends JpaRepository<Horse, Integer> {
    List<Horse> findByOwnerId(Integer ownerId);

    List<Horse> findByOwnerIdAndIsActive(Integer ownerId, Boolean isActive);

    boolean existsByRegisterCode(String registerCode);
}
