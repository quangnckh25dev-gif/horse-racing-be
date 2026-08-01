package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "BetSelections")
public class BetSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SelectionID")
    private Integer selectionId;

    @Column(name = "TicketID", nullable = false)
    private Integer ticketId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "EntryID", nullable = false)
    private Integer entryId;

    @Column(name = "BetType", nullable = false)
    private String betType;

    @Column(name = "TargetPosition")
    private Integer targetPosition;

    @Column(name = "Odds", nullable = false, precision = 10, scale = 2)
    private BigDecimal odds;

    @Column(name = "Resolved", nullable = false)
    private Boolean resolved;

    @Column(name = "Won")
    private Boolean won;

    public Integer getSelectionId() { return selectionId; }
    public void setSelectionId(Integer selectionId) { this.selectionId = selectionId; }
    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }
    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public Integer getEntryId() { return entryId; }
    public void setEntryId(Integer entryId) { this.entryId = entryId; }
    public String getBetType() { return betType; }
    public void setBetType(String betType) { this.betType = betType; }
    public Integer getTargetPosition() { return targetPosition; }
    public void setTargetPosition(Integer targetPosition) { this.targetPosition = targetPosition; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public Boolean getWon() { return won; }
    public void setWon(Boolean won) { this.won = won; }
}
