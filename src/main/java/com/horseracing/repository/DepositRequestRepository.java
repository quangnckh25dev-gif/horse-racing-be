package com.horseracing.repository;

import com.horseracing.entity.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Integer> {
    List<DepositRequest> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<DepositRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByTransferCode(String transferCode);
    Optional<DepositRequest> findByTransferCode(String transferCode);

    @Query(value = """
            SELECT dr.*
            FROM DepositRequests dr
            JOIN Users u ON u.UserID = dr.UserID
            WHERE (:status IS NULL OR dr.Status = :status)
              AND (:paymentMethod IS NULL OR dr.PaymentMethod = :paymentMethod)
              AND (:date IS NULL OR CAST(dr.CreatedAt AS DATE) = :date)
              AND (
                    :keyword IS NULL
                    OR LOWER(ISNULL(u.Username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(ISNULL(u.FullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(ISNULL(u.Email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(ISNULL(u.Phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(ISNULL(dr.TransferCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY dr.CreatedAt DESC
            """, nativeQuery = true)
    List<DepositRequest> searchAdminDepositRequests(@Param("status") String status,
                                                    @Param("paymentMethod") String paymentMethod,
                                                    @Param("date") LocalDate date,
                                                    @Param("keyword") String keyword);

    @Modifying
    @Query("""
            update DepositRequest dr
            set dr.status = 'Rejected',
                dr.adminNote = 'Auto rejected after timeout',
                dr.updatedAt = :now
            where dr.status = 'Pending'
              and dr.createdAt < :expiredBefore
            """)
    int autoRejectExpiredPendingRequests(@Param("expiredBefore") LocalDateTime expiredBefore,
                                         @Param("now") LocalDateTime now);
}
