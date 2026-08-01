package com.horseracing.dto;

import java.math.BigDecimal;
import java.util.List;

public class BetRequest {
    private Integer entryId;
    private String betType;
    private Integer targetPosition;
    private BigDecimal amount;
    private List<SelectionRequest> selections;

    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public String getBetType() { return betType; }
    public void setBetType(String betType) { this.betType = betType; }
    public Integer getTargetPosition() { return targetPosition; }
    public void setTargetPosition(Integer targetPosition) { this.targetPosition = targetPosition; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public List<SelectionRequest> getSelections() { return selections; }
    public void setSelections(List<SelectionRequest> selections) { this.selections = selections; }

    public static class SelectionRequest {
        private Integer entryId;
        private String betType;
        private Integer targetPosition;

        public Integer getEntryId() { return entryId; }
        public void setEntryId(Integer entryId) { this.entryId = entryId; }
        public String getBetType() { return betType; }
        public void setBetType(String betType) { this.betType = betType; }
        public Integer getTargetPosition() { return targetPosition; }
        public void setTargetPosition(Integer targetPosition) { this.targetPosition = targetPosition; }
    }
}
