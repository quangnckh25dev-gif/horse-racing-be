package com.horseracing.dto;

import java.math.BigDecimal;

public class ViolationOptionResponse {
    private String violationType;
    private String label;
    private BigDecimal penaltySeconds;
    private Boolean isDq;

    public ViolationOptionResponse(String violationType, String label, BigDecimal penaltySeconds, Boolean isDq) {
        this.violationType = violationType;
        this.label = label;
        this.penaltySeconds = penaltySeconds;
        this.isDq = isDq;
    }

    public String getViolationType() {
        return violationType;
    }

    public String getType() {
        return violationType;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getPenaltySeconds() {
        return penaltySeconds;
    }

    public BigDecimal getPenalty() {
        return penaltySeconds;
    }

    public Boolean getIsDq() {
        return isDq;
    }
}
