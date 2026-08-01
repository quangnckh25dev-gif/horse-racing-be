package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepositComplaintResponse {
    private Integer complaintId;
    private Integer userId;
    private String username;
    private String fullName;
    private Integer depositRequestId;
    private String transferCode;
    private BigDecimal amount;
    private String paymentMethod;
    private String reason;
    private String evidenceUrl;
    private String status;
    private String adminNote;
    private Integer resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DepositComplaintResponse(Integer complaintId, Integer userId, String username, String fullName,
                                    Integer depositRequestId, String transferCode, BigDecimal amount,
                                    String paymentMethod, String reason, String evidenceUrl, String status,
                                    String adminNote, Integer resolvedBy, LocalDateTime resolvedAt,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.complaintId = complaintId;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.depositRequestId = depositRequestId;
        this.transferCode = transferCode;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.reason = reason;
        this.evidenceUrl = evidenceUrl;
        this.status = status;
        this.adminNote = adminNote;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getComplaintId() { return complaintId; }
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Integer getDepositRequestId() { return depositRequestId; }
    public String getTransferCode() { return transferCode; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getReason() { return reason; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public Integer getResolvedBy() { return resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
