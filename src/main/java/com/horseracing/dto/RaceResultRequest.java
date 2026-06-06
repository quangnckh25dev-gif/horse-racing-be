package com.horseracing.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RaceResultRequest {

    private Integer entryId;

    private Integer position;

    private Integer finishPosition;

    private String finishTime;

    private Integer point;

    private BigDecimal prizeWon;

    private Boolean dnf;

    private Boolean dq;

    private Integer confirmedByRef;

    private String note;

    public Integer getEntryId() {
        return entryId;
    }

    public void setEntryId(Integer entryId) {
        this.entryId = entryId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getFinishPosition() {
        return finishPosition;
    }

    public void setFinishPosition(Integer finishPosition) {
        this.finishPosition = finishPosition;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public BigDecimal getPrizeWon() {
        return prizeWon;
    }

    public void setPrizeWon(BigDecimal prizeWon) {
        this.prizeWon = prizeWon;
    }

    public Boolean getDnf() {
        return dnf;
    }

    public void setDnf(Boolean dnf) {
        this.dnf = dnf;
    }

    public Boolean getDq() {
        return dq;
    }

    public void setDq(Boolean dq) {
        this.dq = dq;
    }

    public Integer getConfirmedByRef() {
        return confirmedByRef;
    }

    public void setConfirmedByRef(Integer confirmedByRef) {
        this.confirmedByRef = confirmedByRef;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
