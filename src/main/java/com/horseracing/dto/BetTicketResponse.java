package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BetTicketResponse {
    private Integer ticketId;
    private Integer userId;
    private Integer raceId;
    private String raceName;
    private BigDecimal amount;
    private BigDecimal odds;
    private BigDecimal potentialPayout;
    private BigDecimal actualPayout;
    private String status;
    private Boolean settled;
    private List<BetSelectionResponse> selections;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    public BetTicketResponse(Integer ticketId, Integer userId, Integer raceId, String raceName,
                             BigDecimal amount, BigDecimal odds, BigDecimal potentialPayout,
                             String status, List<BetSelectionResponse> selections,
                             LocalDateTime createdAt, LocalDateTime settledAt) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.raceId = raceId;
        this.raceName = raceName;
        this.amount = amount;
        this.odds = odds;
        this.potentialPayout = potentialPayout;
        this.actualPayout = "Won".equals(status) ? potentialPayout : "Lost".equals(status) ? BigDecimal.ZERO : null;
        this.status = status;
        this.settled = settledAt != null || "Won".equals(status) || "Lost".equals(status);
        this.selections = selections;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    public Integer getTicketId() { return ticketId; }
    public Integer getUserId() { return userId; }
    public Integer getRaceId() { return raceId; }
    public String getRaceName() { return raceName; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOdds() { return odds; }
    public BigDecimal getPotentialPayout() { return potentialPayout; }
    public BigDecimal getActualPayout() { return actualPayout; }
    public String getStatus() { return status; }
    public Boolean getSettled() { return settled; }
    public List<BetSelectionResponse> getSelections() { return selections; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
}
