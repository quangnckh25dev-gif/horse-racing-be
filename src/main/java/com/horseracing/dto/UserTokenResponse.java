package com.horseracing.dto;

import java.time.LocalDateTime;

public class UserTokenResponse {
    private Integer tokenId;
    private Integer userId;
    private String token;
    private LocalDateTime expiresAt;
    private Boolean isRevoked;
    private LocalDateTime createdAt;

    public UserTokenResponse(Integer tokenId, Integer userId, String token, LocalDateTime expiresAt,
                             Boolean isRevoked, LocalDateTime createdAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.isRevoked = isRevoked;
        this.createdAt = createdAt;
    }

    public Integer getTokenId() {
        return tokenId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
