package com.horseracing.dto;

import java.math.BigDecimal;

public class WalletDepositRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
