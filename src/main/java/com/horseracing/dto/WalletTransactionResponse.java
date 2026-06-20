package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {
    private Integer transactionId;
    private Integer walletId;
    private BigDecimal amount;
    private String transactionType;
    private String description;
    private String relatedEntity;
    private Integer relatedEntityId;
    private LocalDateTime createdAt;

    public WalletTransactionResponse(Integer transactionId, Integer walletId, BigDecimal amount,
                                     String transactionType, String description, String relatedEntity,
                                     Integer relatedEntityId, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
        this.relatedEntity = relatedEntity;
        this.relatedEntityId = relatedEntityId;
        this.createdAt = createdAt;
    }

    public Integer getTransactionId() { return transactionId; }
    public Integer getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getTransactionType() { return transactionType; }
    public String getDescription() { return description; }
    public String getRelatedEntity() { return relatedEntity; }
    public Integer getRelatedEntityId() { return relatedEntityId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
