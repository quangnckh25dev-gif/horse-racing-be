package com.horseracing.dto;

import java.math.BigDecimal;

public class WalletTransactionSummaryResponse {
    private final BigDecimal currentBalance;
    private final BigDecimal totalMoneyIn;
    private final BigDecimal totalMoneyOut;
    private final Integer totalTransactions;

    public WalletTransactionSummaryResponse(BigDecimal currentBalance, BigDecimal totalMoneyIn,
                                            BigDecimal totalMoneyOut, Integer totalTransactions) {
        this.currentBalance = currentBalance;
        this.totalMoneyIn = totalMoneyIn;
        this.totalMoneyOut = totalMoneyOut;
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getTotalMoneyIn() { return totalMoneyIn; }
    public BigDecimal getTotalMoneyOut() { return totalMoneyOut; }
    public Integer getTotalTransactions() { return totalTransactions; }
}
