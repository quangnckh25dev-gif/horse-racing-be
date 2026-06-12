package com.horseracing.dto;

import java.time.LocalDateTime;

public class UserRoleHistoryResponse {
    private Integer historyId;
    private Integer userId;
    private String username;
    private Integer oldRoleId;
    private String oldRoleName;
    private Integer newRoleId;
    private String newRoleName;
    private Integer changedBy;
    private String changedByUsername;
    private LocalDateTime changedAt;

    public UserRoleHistoryResponse(Integer historyId, Integer userId, String username, Integer oldRoleId,
                                   String oldRoleName, Integer newRoleId, String newRoleName, Integer changedBy,
                                   String changedByUsername, LocalDateTime changedAt) {
        this.historyId = historyId;
        this.userId = userId;
        this.username = username;
        this.oldRoleId = oldRoleId;
        this.oldRoleName = oldRoleName;
        this.newRoleId = newRoleId;
        this.newRoleName = newRoleName;
        this.changedBy = changedBy;
        this.changedByUsername = changedByUsername;
        this.changedAt = changedAt;
    }

    public Integer getHistoryId() {
        return historyId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Integer getOldRoleId() {
        return oldRoleId;
    }

    public String getOldRoleName() {
        return oldRoleName;
    }

    public Integer getNewRoleId() {
        return newRoleId;
    }

    public String getNewRoleName() {
        return newRoleName;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
