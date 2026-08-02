package com.horseracing.repository;

import com.horseracing.entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Integer> {
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();
}
