package com.horseracing.dto;

public class RaceResultRequest {

    private Integer entryId;

    private String finishTime;

    private Boolean dnf;

    private String note;

    public Integer getEntryId() {
        return entryId;
    }

    public void setEntryId(Integer entryId) {
        this.entryId = entryId;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public Boolean getDnf() {
        return dnf;
    }

    public void setDnf(Boolean dnf) {
        this.dnf = dnf;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
