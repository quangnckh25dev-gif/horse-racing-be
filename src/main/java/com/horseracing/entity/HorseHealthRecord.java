package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "HorseHealthRecords")
public class HorseHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RecordID")
    private Integer recordId;

    @Column(name = "HorseID", nullable = false)
    private Integer horseId;

    @Column(name = "CheckDate", nullable = false)
    private LocalDate checkDate;

    @Column(name = "VetName")
    private String vetName;

    @Column(name = "HealthStatus")
    private String healthStatus;

    @Column(name = "Diagnosis")
    private String diagnosis;

    @Column(name = "Notes")
    private String notes;

    @Column(name = "EvidenceUrl")
    private String evidenceUrl;

    @Column(name = "Status")
    private String status;

    @Column(name = "SubmittedBy")
    private Integer submittedBy;

    @Column(name = "RecordedBy")
    private Integer recordedBy;

    @Column(name = "ReviewedBy")
    private Integer reviewedBy;

    @Column(name = "ReviewedAt")
    private LocalDateTime reviewedAt;

    @Column(name = "ReviewNote")
    private String reviewNote;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }
    public Integer getHorseId() { return horseId; }
    public void setHorseId(Integer horseId) { this.horseId = horseId; }
    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }
    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(Integer submittedBy) { this.submittedBy = submittedBy; }
    public Integer getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Integer recordedBy) { this.recordedBy = recordedBy; }
    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
