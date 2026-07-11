package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RaceEntryResponse {
    private Integer entryId;
    private Integer raceId;
    private String raceName;
    private Integer horseId;
    private String horseName;
    private Integer ownerId;
    private String ownerName;
    private Integer jockeyId;
    private String jockeyName;
    private Integer laneNumber;
    private String registrationStatus;
    private Integer approvedBy;
    private String rejectReason;
    private Boolean jockeyConfirmed;
    private BigDecimal odds;
    private String healthStatus;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;

    public RaceEntryResponse(Integer entryId, Integer raceId, String raceName, Integer horseId, String horseName,
                             Integer ownerId, String ownerName, Integer jockeyId, String jockeyName,
                             Integer laneNumber, String registrationStatus, Integer approvedBy,
                             String rejectReason, Boolean jockeyConfirmed, BigDecimal odds, String healthStatus,
                             LocalDateTime registeredAt,
                             LocalDateTime updatedAt) {
        this.entryId = entryId;
        this.raceId = raceId;
        this.raceName = raceName;
        this.horseId = horseId;
        this.horseName = horseName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.jockeyId = jockeyId;
        this.jockeyName = jockeyName;
        this.laneNumber = laneNumber;
        this.registrationStatus = registrationStatus;
        this.approvedBy = approvedBy;
        this.rejectReason = rejectReason;
        this.jockeyConfirmed = jockeyConfirmed;
        this.odds = odds;
        this.healthStatus = healthStatus;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
    }

    public Integer getEntryId() { return entryId; }
    public Integer getRaceId() { return raceId; }
    public String getRaceName() { return raceName; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Integer getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public Integer getJockeyId() { return jockeyId; }
    public String getJockeyName() { return jockeyName; }
    public Integer getLaneNumber() { return laneNumber; }
    public String getRegistrationStatus() { return registrationStatus; }
    public Integer getApprovedBy() { return approvedBy; }
    public String getRejectReason() { return rejectReason; }
    public Boolean getJockeyConfirmed() { return jockeyConfirmed; }
    public BigDecimal getOdds() { return odds; }
    public String getHealthStatus() { return healthStatus; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
