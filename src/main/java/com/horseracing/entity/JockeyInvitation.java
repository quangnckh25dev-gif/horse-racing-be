package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "JockeyInvitations")
public class JockeyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvitationID")
    private Integer invitationId;

    @Column(name = "EntryID", nullable = false)
    private Integer entryId;

    @Column(name = "JockeyID", nullable = false)
    private Integer jockeyId;

    @Column(name = "InvitedByOwner", nullable = false)
    private Integer invitedByOwner;

    @Column(name = "DealAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal dealAmount;

    @Column(name = "Message")
    private String message;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "ResponseReason")
    private String responseReason;

    @Column(name = "InvitedAt")
    private LocalDateTime invitedAt;

    @Column(name = "RespondedAt")
    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        invitedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = "Pending";
        }
    }

    public Integer getInvitationId() { return invitationId; }
    public void setInvitationId(Integer invitationId) { this.invitationId = invitationId; }
    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public Integer getJockeyId() { return jockeyId; }
    public void setJockeyId(Integer jockeyId) { this.jockeyId = jockeyId; }
    public Integer getInvitedByOwner() { return invitedByOwner; }
    public void setInvitedByOwner(Integer invitedByOwner) { this.invitedByOwner = invitedByOwner; }
    public BigDecimal getDealAmount() { return dealAmount; }
    public void setDealAmount(BigDecimal dealAmount) { this.dealAmount = dealAmount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResponseReason() { return responseReason; }
    public void setResponseReason(String responseReason) { this.responseReason = responseReason; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
