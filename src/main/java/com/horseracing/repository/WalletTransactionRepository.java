package com.horseracing.repository;

import com.horseracing.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Integer walletId);

    boolean existsByWalletIdAndTransactionTypeAndRelatedEntityAndRelatedEntityId(
            Integer walletId,
            String transactionType,
            String relatedEntity,
            Integer relatedEntityId
    );
}
