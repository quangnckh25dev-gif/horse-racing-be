package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class JockeyInvitationResponse {
    private Integer invitationId;
    private Integer entryId;
    private Integer raceId;
    private String raceName;
    private Integer horseId;
    private String horseName;
    private Integer jockeyId;
    private String jockeyName;
    private Integer ownerId;
    private String ownerName;
    private BigDecimal dealAmount;
    private String message;
    private String status;
    private String responseReason;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;

    public JockeyInvitationResponse(Integer invitationId, Integer entryId, Integer raceId, String raceName,
                                    Integer horseId, String horseName, Integer jockeyId, String jockeyName,
                                    Integer ownerId, String ownerName, BigDecimal dealAmount, String message,
                                    String status, String responseReason, LocalDateTime invitedAt,
                                    LocalDateTime respondedAt) {
        this.invitationId = invitationId;
        this.entryId = entryId;
        this.raceId = raceId;
        this.raceName = raceName;
        this.horseId = horseId;
        this.horseName = horseName;
        this.jockeyId = jockeyId;
        this.jockeyName = jockeyName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.dealAmount = dealAmount;
        this.message = message;
        this.status = status;
        this.responseReason = responseReason;
        this.invitedAt = invitedAt;
        this.respondedAt = respondedAt;
    }

    public Integer getInvitationId() { return invitationId; }
    public Integer getEntryId() { return entryId; }
    public Integer getRaceId() { return raceId; }
    public String getRaceName() { return raceName; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Integer getJockeyId() { return jockeyId; }
    public String getJockeyName() { return jockeyName; }
    public Integer getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getDealAmount() { return dealAmount; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getResponseReason() { return responseReason; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
}
