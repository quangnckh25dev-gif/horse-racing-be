package com.horseracing.dto;

import java.time.LocalDateTime;

public class PreRaceCheckResponse {
    private Integer preRaceCheckId;
    private Integer raceId;
    private Integer entryId;
    private Integer horseId;
    private String horseName;
    private Integer refereeId;
    private String status;
    private String reason;
    private LocalDateTime checkedAt;

    public PreRaceCheckResponse(Integer preRaceCheckId, Integer raceId, Integer entryId, Integer horseId,
                                String horseName, Integer refereeId, String status, String reason,
                                LocalDateTime checkedAt) {
        this.preRaceCheckId = preRaceCheckId;
        this.raceId = raceId;
        this.entryId = entryId;
        this.horseId = horseId;
        this.horseName = horseName;
        this.refereeId = refereeId;
        this.status = status;
        this.reason = reason;
        this.checkedAt = checkedAt;
    }

    public Integer getPreRaceCheckId() { return preRaceCheckId; }
    public Integer getRaceId() { return raceId; }
    public Integer getEntryId() { return entryId; }
    public Integer getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public Integer getRefereeId() { return refereeId; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
}
