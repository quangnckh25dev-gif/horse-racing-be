package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {
    private Integer walletId;
    private Integer userId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WalletResponse(Integer walletId, Integer userId, BigDecimal balance,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.currency = "VND";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getWalletId() { return walletId; }
    public Integer getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
