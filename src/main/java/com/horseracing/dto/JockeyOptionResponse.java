package com.horseracing.dto;

public class JockeyOptionResponse {
    private Integer jockeyId;
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private Integer totalRaces;
    private Integer totalWins;

    public JockeyOptionResponse(Integer jockeyId, Integer userId, String fullName, String email,
                                String phone, Integer totalRaces, Integer totalWins) {
        this.jockeyId = jockeyId;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.totalRaces = totalRaces;
        this.totalWins = totalWins;
    }

    public Integer getJockeyId() { return jockeyId; }
    public Integer getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Integer getTotalRaces() { return totalRaces; }
    public Integer getTotalWins() { return totalWins; }
}