package com.horseracing.dto;

import java.time.LocalDateTime;

public class SystemConfigResponse {
    private final Integer configId;
    private final String configKey;
    private final String configValue;
    private final String description;
    private final Integer updatedBy;
    private final LocalDateTime updatedAt;

    public SystemConfigResponse(
            Integer configId,
            String configKey,
            String configValue,
            String description,
            Integer updatedBy,
            LocalDateTime updatedAt
    ) {
        this.configId = configId;
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Integer getConfigId() { return configId; }
    public String getConfigKey() { return configKey; }
    public String getConfigValue() { return configValue; }
    public String getDescription() { return description; }
    public Integer getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
