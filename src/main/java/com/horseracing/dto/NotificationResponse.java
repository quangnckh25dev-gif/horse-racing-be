package com.horseracing.dto;

import java.time.LocalDateTime;

public class NotificationResponse {
    private Integer notificationId;
    private Integer userId;
    private String title;
    private String body;
    private String notifType;
    private Integer relatedEntityId;
    private String relatedEntity;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public NotificationResponse(Integer notificationId, Integer userId, String title, String body,
                                String notifType, Integer relatedEntityId, String relatedEntity,
                                Boolean isRead, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.notifType = notifType;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntity = relatedEntity;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public Integer getNotificationId() {
        return notificationId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getNotifType() {
        return notifType;
    }

    public Integer getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getRelatedEntity() {
        return relatedEntity;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
