package com.horseracing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "RaceStatusHistory")
public class RaceStatusHistory {
    //của buiquangann
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer historyId;
    @Column(name = "RaceID", nullable = false)
    private Integer raceId;
    @Column(name = "OldStatus")
    private String oldStatus;
    @Column(name = "NewStatus", nullable = false)
    private String newStatus;
    @Column(name = "ChangedBy")
    private Integer changedBy;
    @Column(name = "ChangedAt")
    private LocalDateTime changedAt;

    @PrePersist void prePersist() { changedAt = LocalDateTime.now(); }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public void setChangedBy(Integer changedBy) { this.changedBy = changedBy; }
}
