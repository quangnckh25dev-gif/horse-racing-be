package com.horseracing.dto;

import java.time.LocalDateTime;

public class RaceComplaintResponse {
    private Integer complaintId;
    private Integer ownerUserId;
    private String ownerUsername;
    private String ownerFullName;
    private Integer raceId;
    private String raceName;
    private Integer entryId;
    private Integer horseId;
    private String horseName;
    private String reason;
    private String evidenceUrl;
    private String status;
    private Integer refereeId;
    private String refereeName;
    private String refereeNote;
    private Integer organizerId;
    private String organizerName;
    private String organizerNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public RaceComplaintResponse(Integer complaintId, Integer ownerUserId, String ownerUsername, String ownerFullName,
                                 Integer raceId, String raceName, Integer entryId, Integer horseId, String horseName,
                                 String reason, String evidenceUrl, String status, Integer refereeId,
                                 String refereeName, String refereeNote, Integer organizerId, String organizerName,
                                 String organizerNote, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.complaintId = complaintId;
        this.ownerUserId = ownerUserId;
        this.ownerUsername = ownerUsername;
        this.ownerFullName = ownerFullName;
        this.raceId = raceId;
        this.raceName = raceName;
        this.entryId = entryId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.reason = reason;
        this.evidenceUrl = evidenceUrl;
        this.status = status;
        this.refereeId = refereeId;
        this.refereeName = refereeName;
        this.refereeNote = refereeNote;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.organizerNote = organizerNote;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Integer getComplaintId() { return complaintId; }
    public Integer getOwnerUserId() { return ownerUserId; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getOwnerFullName() { return ownerFullName; }
    public Integer getRaceId() { return raceId; }
    public String getRaceName() { return raceName; }
    public Integer getEntryId() { return entryId; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public String getReason() { return reason; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getStatus() { return status; }
    public Integer getRefereeId() { return refereeId; }
    public String getRefereeName() { return refereeName; }
    public String getRefereeNote() { return refereeNote; }
    public Integer getOrganizerId() { return organizerId; }
    public String getOrganizerName() { return organizerName; }
    public String getOrganizerNote() { return organizerNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
