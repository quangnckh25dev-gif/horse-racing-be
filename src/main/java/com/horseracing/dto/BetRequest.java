package com.horseracing.dto;

import java.math.BigDecimal;

public class BetRequest {
    private Integer entryId;
    private BigDecimal amount;
    private BigDecimal odds;

    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
}
