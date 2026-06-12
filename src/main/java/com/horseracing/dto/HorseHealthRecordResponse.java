package com.horseracing.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HorseHealthRecordResponse {
    private Integer recordId;
    private Integer horseId;
    private LocalDate checkDate;
    private String vetName;
    private String diagnosis;
    private String notes;
    private LocalDateTime createdAt;

    public HorseHealthRecordResponse(Integer recordId, Integer horseId, LocalDate checkDate, String vetName,
                                     String diagnosis, String notes, LocalDateTime createdAt) {
        this.recordId = recordId;
        this.horseId = horseId;
        this.checkDate = checkDate;
        this.vetName = vetName;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Integer getRecordId() { return recordId; }
    public Integer getHorseId() { return horseId; }
    public LocalDate getCheckDate() { return checkDate; }
    public String getVetName() { return vetName; }
    public String getDiagnosis() { return diagnosis; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
