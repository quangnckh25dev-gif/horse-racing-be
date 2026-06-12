package com.horseracing.repository;

import com.horseracing.entity.HorseOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HorseOwnerRepository extends JpaRepository<HorseOwner, Integer> {
    Optional<HorseOwner> findByUserId(Integer userId);
}
