package com.horseracing.dto;

public class RaceEntryApproveRequest {
    private Boolean approved;
    private String reason;

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}