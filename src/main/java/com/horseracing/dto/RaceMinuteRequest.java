package com.horseracing.dto;

public class RaceMinuteRequest {

    private Integer refereeId;
    private String content;
    private String preRaceChecks;
    private String postRaceNotes;
    private String note;

    public Integer getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(Integer refereeId) {
        this.refereeId = refereeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPreRaceChecks() {
        return preRaceChecks;
    }

    public void setPreRaceChecks(String preRaceChecks) {
        this.preRaceChecks = preRaceChecks;
    }

    public String getPostRaceNotes() {
        return postRaceNotes;
    }

    public void setPostRaceNotes(String postRaceNotes) {
        this.postRaceNotes = postRaceNotes;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
