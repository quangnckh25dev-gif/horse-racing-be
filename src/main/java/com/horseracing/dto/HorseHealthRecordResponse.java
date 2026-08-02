package com.horseracing.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HorseHealthRecordResponse {
    private Integer recordId;
    private Integer horseId;
    private LocalDate checkDate;
    private String vetName;
    private String healthStatus;
    private String diagnosis;
    private String notes;
    private String evidenceUrl;
    private String status;
    private Integer submittedBy;
    private Integer recordedBy;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime createdAt;

    public HorseHealthRecordResponse(Integer recordId, Integer horseId, LocalDate checkDate, String vetName,
                                     String diagnosis, String notes, LocalDateTime createdAt) {
        this(recordId, horseId, checkDate, vetName, null, diagnosis, notes, null, null, null, null, null, null, null, createdAt);
    }

    public HorseHealthRecordResponse(Integer recordId, Integer horseId, LocalDate checkDate, String vetName,
                                     String healthStatus, String diagnosis, String notes, String evidenceUrl,
                                     String status, Integer submittedBy, Integer recordedBy, Integer reviewedBy,
                                     LocalDateTime reviewedAt, String reviewNote, LocalDateTime createdAt) {
        this.recordId = recordId;
        this.horseId = horseId;
        this.checkDate = checkDate;
        this.vetName = vetName;
        this.healthStatus = healthStatus;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.evidenceUrl = evidenceUrl;
        this.status = status;
        this.submittedBy = submittedBy;
        this.recordedBy = recordedBy;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNote = reviewNote;
        this.createdAt = createdAt;
    }

    public Integer getRecordId() { return recordId; }
    public Integer getHorseId() { return horseId; }
    public LocalDate getCheckDate() { return checkDate; }
    public String getVetName() { return vetName; }
    public String getHealthStatus() { return healthStatus; }
    public String getDiagnosis() { return diagnosis; }
    public String getNotes() { return notes; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getStatus() { return status; }
    public Integer getSubmittedBy() { return submittedBy; }
    public Integer getRecordedBy() { return recordedBy; }
    public Integer getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
