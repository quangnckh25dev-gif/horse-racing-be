package com.horseracing.dto;

import java.time.LocalDateTime;

public class AuditLogResponse {
    private final Integer logId;
    private final Integer userId;
    private final String action;
    private final String tableName;
    private final Integer recordId;
    private final String oldValue;
    private final String newValue;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public AuditLogResponse(
            Integer logId,
            Integer userId,
            String action,
            String tableName,
            Integer recordId,
            String oldValue,
            String newValue,
            String ipAddress,
            LocalDateTime createdAt
    ) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.tableName = tableName;
        this.recordId = recordId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Integer getLogId() { return logId; }
    public Integer getUserId() { return userId; }
    public String getAction() { return action; }
    public String getTableName() { return tableName; }
    public Integer getRecordId() { return recordId; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
