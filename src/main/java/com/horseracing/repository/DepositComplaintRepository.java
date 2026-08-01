package com.horseracing.repository;

import com.horseracing.entity.DepositComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositComplaintRepository extends JpaRepository<DepositComplaint, Integer> {
    List<DepositComplaint> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<DepositComplaint> findAllByOrderByCreatedAtDesc();
}
