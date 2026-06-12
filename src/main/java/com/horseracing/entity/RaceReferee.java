package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "RaceReferees",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"RaceID", "RefereeID"})
        }
)
public class RaceReferee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RaceRefereeID")
    private Integer raceRefereeId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "RefereeID", nullable = false)
    private Integer refereeId;

    @Column(name = "Role")
    private String role;

    @Column(name = "AssignedAt")
    private LocalDateTime assignedAt;

    @PrePersist
    public void prePersist() {
        assignedAt = LocalDateTime.now();
    }

    public Integer getRaceRefereeId() {
        return raceRefereeId;
    }

    public void setRaceRefereeId(Integer raceRefereeId) {
        this.raceRefereeId = raceRefereeId;
    }

    public Integer getRaceId() {
        return raceId;
    }

    public void setRaceId(Integer raceId) {
        this.raceId = raceId;
    }

    public Integer getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(Integer refereeId) {
        this.refereeId = refereeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
