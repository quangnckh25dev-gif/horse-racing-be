package com.horseracing.dto;

public class RaceComplaintRequest {
    private Integer raceId;
    private Integer entryId;
    private String reason;
    private String evidenceUrl;
    private String refereeNote;
    private String organizerNote;
    private Boolean resultCorrectionRequired;

    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public String getRefereeNote() { return refereeNote; }
    public void setRefereeNote(String refereeNote) { this.refereeNote = refereeNote; }
    public String getOrganizerNote() { return organizerNote; }
    public void setOrganizerNote(String organizerNote) { this.organizerNote = organizerNote; }
    public Boolean getResultCorrectionRequired() { return resultCorrectionRequired; }
    public void setResultCorrectionRequired(Boolean resultCorrectionRequired) { this.resultCorrectionRequired = resultCorrectionRequired; }
}
