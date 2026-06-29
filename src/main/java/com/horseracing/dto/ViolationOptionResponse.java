package com.horseracing.dto;

public class ViolationOptionResponse {
    private String violationType;
    private String label;
    private String penalty;

    public ViolationOptionResponse(String violationType, String label, String penalty) {
        this.violationType = violationType;
        this.label = label;
        this.penalty = penalty;
    }

    public String getViolationType() {
        return violationType;
    }

    public String getLabel() {
        return label;
    }

    public String getPenalty() {
        return penalty;
    }
}
