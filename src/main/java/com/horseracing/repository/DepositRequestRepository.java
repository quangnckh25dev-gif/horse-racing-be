package com.horseracing.repository;

import com.horseracing.entity.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Integer> {
    List<DepositRequest> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<DepositRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByTransferCode(String transferCode);
}
