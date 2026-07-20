package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {
    private Integer transactionId;
    private Integer walletId;
    private BigDecimal amount;
    private String direction;
    private String currency;
    private String transactionType;
    private String transactionTypeLabel;
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
        this.direction = amount != null && amount.compareTo(BigDecimal.ZERO) < 0 ? "OUT" : "IN";
        this.currency = "VND";
        this.transactionType = transactionType;
        this.transactionTypeLabel = toTransactionTypeLabel(transactionType);
        this.description = description;
        this.relatedEntity = relatedEntity;
        this.relatedEntityId = relatedEntityId;
        this.createdAt = createdAt;
    }

    public Integer getTransactionId() { return transactionId; }
    public Integer getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getDirection() { return direction; }
    public String getCurrency() { return currency; }
    public String getTransactionType() { return transactionType; }
    public String getType() { return transactionType; }
    public String getTransactionTypeLabel() { return transactionTypeLabel; }
    public String getDescription() { return description; }
    public String getRelatedEntity() { return relatedEntity; }
    public Integer getRelatedEntityId() { return relatedEntityId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private String toTransactionTypeLabel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "Deposit" -> "Deposit";
            case "BetPlaced" -> "Bet Placed";
            case "BetWon" -> "Bet Won";
            default -> value;
        };
    }
}
