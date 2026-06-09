package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserRoleHistory")
public class UserRoleHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer historyId;

    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "OldRoleID")
    private Integer oldRoleId;

    @Column(name = "NewRoleID")
    private Integer newRoleId;

    @Column(name = "ChangedBy")
    private Integer changedBy;

    @Column(name = "ChangedAt")
    private LocalDateTime changedAt;

    public Integer getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Integer historyId) {
        this.historyId = historyId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getOldRoleId() {
        return oldRoleId;
    }

    public void setOldRoleId(Integer oldRoleId) {
        this.oldRoleId = oldRoleId;
    }

    public Integer getNewRoleId() {
        return newRoleId;
    }

    public void setNewRoleId(Integer newRoleId) {
        this.newRoleId = newRoleId;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
