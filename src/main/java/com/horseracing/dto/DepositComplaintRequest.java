package com.horseracing.dto;

import java.math.BigDecimal;

public class DepositComplaintRequest {
    private Integer depositRequestId;
    private String transferCode;
    private BigDecimal amount;
    private String paymentMethod;
    private String reason;
    private String evidenceUrl;
    private String adminNote;

    public Integer getDepositRequestId() { return depositRequestId; }
    public void setDepositRequestId(Integer depositRequestId) { this.depositRequestId = depositRequestId; }
    public String getTransferCode() { return transferCode; }
    public void setTransferCode(String transferCode) { this.transferCode = transferCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
