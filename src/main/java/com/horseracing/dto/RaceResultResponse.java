package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RaceResultResponse {

    private Integer resultId;

    private Integer raceId;

    private Integer entryId;

    private Integer position;

    private String finishTime;

    private Integer point;

    private BigDecimal prizeWon;

    private Boolean dnf;

    private Boolean dq;

    private Integer confirmedByRef;

    private LocalDateTime confirmedAt;

    private String approvalStatus;

    private Integer approvedByOrganizer;

    private LocalDateTime approvedAt;

    private LocalDateTime publishedAt;

    private Boolean published;

    private LocalDateTime createdAt;

    public RaceResultResponse(Integer resultId, Integer raceId, Integer entryId, Integer position,
                              String finishTime, Integer point, BigDecimal prizeWon, Boolean dnf, Boolean dq,
                              Integer confirmedByRef, LocalDateTime confirmedAt, String approvalStatus,
                              Integer approvedByOrganizer, LocalDateTime approvedAt, LocalDateTime publishedAt,
                              Boolean published, LocalDateTime createdAt) {
        this.resultId = resultId;
        this.raceId = raceId;
        this.entryId = entryId;
        this.position = position;
        this.finishTime = finishTime;
        this.point = point;
        this.prizeWon = prizeWon;
        this.dnf = dnf;
        this.dq = dq;
        this.confirmedByRef = confirmedByRef;
        this.confirmedAt = confirmedAt;
        this.approvalStatus = approvalStatus;
        this.approvedByOrganizer = approvedByOrganizer;
        this.approvedAt = approvedAt;
        this.publishedAt = publishedAt;
        this.published = published;
        this.createdAt = createdAt;
    }

    public Integer getResultId() {
        return resultId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public Integer getEntryId() {
        return entryId;
    }

    public Integer getPosition() {
        return position;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public Integer getPoint() {
        return point;
    }

    public BigDecimal getPrizeWon() {
        return prizeWon;
    }

    public Boolean getDnf() {
        return dnf;
    }

    public Boolean getDq() {
        return dq;
    }

    public Integer getConfirmedByRef() {
        return confirmedByRef;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public Integer getApprovedByOrganizer() {
        return approvedByOrganizer;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Boolean getPublished() {
        return published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
