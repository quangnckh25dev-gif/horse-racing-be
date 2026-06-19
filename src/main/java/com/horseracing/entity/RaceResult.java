package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "RaceResults",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"RaceID", "EntryID"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ResultID")
    private Integer resultId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "EntryID", nullable = false)
    private Integer entryId;

    @Column(name = "FinishPosition")
    private Integer finishPosition;

    @Column(name = "FinishTime", precision = 10, scale = 3)
    private BigDecimal finishTime;

    @Column(name = "PrizeWon", precision = 18, scale = 2)
    private BigDecimal prizeWon;

    @Column(name = "DNF", nullable = false)
    private Boolean dnf;

    @Column(name = "DQ", nullable = false)
    private Boolean dq;

    @Column(name = "ConfirmedByRef")
    private Integer confirmedByRef;

    @Column(name = "ConfirmedAt")
    private LocalDateTime confirmedAt;

    @Column(name = "ApprovalStatus", nullable = false)
    private String approvalStatus;

    @Column(name = "ApprovedByOrganizer")
    private Integer approvedByOrganizer;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @Column(name = "PublishedAt")
    private LocalDateTime publishedAt;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public void setRaceId(Integer raceId) {
        this.raceId = raceId;
    }

    public Integer getEntryId() {
        return entryId;
    }

    public void setEntryId(Integer entryId) {
        this.entryId = entryId;
    }

    public Integer getFinishPosition() {
        return finishPosition;
    }

    public void setFinishPosition(Integer finishPosition) {
        this.finishPosition = finishPosition;
    }

    public BigDecimal getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(BigDecimal finishTime) {
        this.finishTime = finishTime;
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

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Integer getApprovedByOrganizer() {
        return approvedByOrganizer;
    }

    public void setApprovedByOrganizer(Integer approvedByOrganizer) {
        this.approvedByOrganizer = approvedByOrganizer;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
