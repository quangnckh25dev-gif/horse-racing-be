package com.horseracing.dto;

import java.time.LocalDate;

public class HorseHealthRecordRequest {
    private LocalDate checkDate;
    private String vetName;
    private String healthStatus;
    private String diagnosis;
    private String note;
    private String notes;

    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }
    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
