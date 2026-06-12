package com.horseracing.dto;

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
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;

    public JockeyInvitationResponse(Integer invitationId, Integer entryId, Integer raceId, String raceName,
                                    Integer horseId, String horseName, Integer jockeyId, String jockeyName,
                                    Integer ownerId, String ownerName, String status,
                                    LocalDateTime invitedAt, LocalDateTime respondedAt) {
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
        this.status = status;
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
    public String getStatus() { return status; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
}