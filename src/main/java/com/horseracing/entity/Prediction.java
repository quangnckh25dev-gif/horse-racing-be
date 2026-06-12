package com.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "Predictions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"UserID", "RaceID"})
        }
)
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PredictionID")
    private Integer predictionId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "RaceID", nullable = false)
    private Integer raceId;

    @Column(name = "PredictedFirst")
    private Integer predictedFirst;

    @Column(name = "PredictedSecond")
    private Integer predictedSecond;

    @Column(name = "PredictedThird")
    private Integer predictedThird;

    @Column(name = "IsCorrect")
    private Boolean isCorrect;

    @Column(name = "RewardAmount", precision = 18, scale = 2)
    private BigDecimal rewardAmount;

    @Column(name = "RewardPaid", nullable = false)
    private Boolean rewardPaid;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (rewardAmount == null) {
            rewardAmount = BigDecimal.ZERO;
        }
        if (rewardPaid == null) {
            rewardPaid = false;
        }
    }

    public Integer getPredictionId() { return predictionId; }
    public void setPredictionId(Integer predictionId) { this.predictionId = predictionId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getRaceId() { return raceId; }
    public void setRaceId(Integer raceId) { this.raceId = raceId; }
    public Integer getPredictedFirst() { return predictedFirst; }
    public void setPredictedFirst(Integer predictedFirst) { this.predictedFirst = predictedFirst; }
    public Integer getPredictedSecond() { return predictedSecond; }
    public void setPredictedSecond(Integer predictedSecond) { this.predictedSecond = predictedSecond; }
    public Integer getPredictedThird() { return predictedThird; }
    public void setPredictedThird(Integer predictedThird) { this.predictedThird = predictedThird; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean correct) { isCorrect = correct; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(BigDecimal rewardAmount) { this.rewardAmount = rewardAmount; }
    public Boolean getRewardPaid() { return rewardPaid; }
    public void setRewardPaid(Boolean rewardPaid) { this.rewardPaid = rewardPaid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
