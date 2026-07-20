package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepositRequestResponse {
    private Integer depositRequestId;
    private Integer userId;
    private Integer walletId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transferCode;
    private String qrCodeUrl;
    private String status;
    private String adminNote;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DepositRequestResponse(Integer depositRequestId, Integer userId, Integer walletId,
                                  BigDecimal amount, String paymentMethod, String transferCode,
                                  String qrCodeUrl, String status, String adminNote, Integer approvedBy,
                                  LocalDateTime approvedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.depositRequestId = depositRequestId;
        this.userId = userId;
        this.walletId = walletId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transferCode = transferCode;
        this.qrCodeUrl = qrCodeUrl;
        this.status = status;
        this.adminNote = adminNote;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getDepositRequestId() { return depositRequestId; }
    public Integer getUserId() { return userId; }
    public Integer getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransferCode() { return transferCode; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public String getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public Integer getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
