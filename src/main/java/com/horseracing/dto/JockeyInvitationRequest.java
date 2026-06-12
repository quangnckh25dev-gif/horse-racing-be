package com.horseracing.dto;

public class JockeyInvitationRequest {
    private Integer jockeyId;
    private String message;

    public Integer getJockeyId() { return jockeyId; }
    public void setJockeyId(Integer jockeyId) { this.jockeyId = jockeyId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}