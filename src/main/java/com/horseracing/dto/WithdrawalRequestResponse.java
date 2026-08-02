package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WithdrawalRequestResponse {
    private Integer withdrawalRequestId;
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Integer walletId;
    private BigDecimal amount;
    private String paymentMethod;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String status;
    private String adminNote;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WithdrawalRequestResponse(Integer withdrawalRequestId, Integer userId, String username, String fullName,
                                     String email, String phone, Integer walletId, BigDecimal amount, String paymentMethod,
                                     String bankName, String bankAccountNumber, String bankAccountName, String status,
                                     String adminNote, Integer approvedBy, LocalDateTime approvedAt,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.withdrawalRequestId = withdrawalRequestId;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.walletId = walletId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.bankAccountName = bankAccountName;
        this.status = status;
        this.adminNote = adminNote;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getWithdrawalRequestId() { return withdrawalRequestId; }
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Integer getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getBankName() { return bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public String getBankAccountName() { return bankAccountName; }
    public String getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public Integer getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
