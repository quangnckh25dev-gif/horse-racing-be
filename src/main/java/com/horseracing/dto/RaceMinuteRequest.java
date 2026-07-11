package com.horseracing.dto;

public class RaceMinuteRequest {

    private String content;
    private String preRaceChecks;
    private String postRaceNotes;
    private String minutesFileUrl;
    private String note;

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

    public String getMinutesFileUrl() {
        return minutesFileUrl;
    }

    public void setMinutesFileUrl(String minutesFileUrl) {
        this.minutesFileUrl = minutesFileUrl;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
