package com.horseracing.dto;

import java.math.BigDecimal;

public class JockeyInvitationRequest {
    private Integer jockeyId;
    private BigDecimal dealAmount;
    private String message;

    public Integer getJockeyId() { return jockeyId; }
    public void setJockeyId(Integer jockeyId) { this.jockeyId = jockeyId; }
    public BigDecimal getDealAmount() { return dealAmount; }
    public void setDealAmount(BigDecimal dealAmount) { this.dealAmount = dealAmount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
