package com.horseracing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PredictionResponse {
    private final Integer predictionId;
    private final Integer userId;
    private final Integer raceId;
    private final Integer predictedWinnerEntryId;
    private final Integer predictedSecondEntryId;
    private final Integer predictedThirdEntryId;
    private final Boolean isCorrect;
    private final BigDecimal rewardAmount;
    private final Boolean rewardPaid;
    private final LocalDateTime createdAt;

    public PredictionResponse(
            Integer predictionId,
            Integer userId,
            Integer raceId,
            Integer predictedWinnerEntryId,
            Integer predictedSecondEntryId,
            Integer predictedThirdEntryId,
            Boolean isCorrect,
            BigDecimal rewardAmount,
            Boolean rewardPaid,
            LocalDateTime createdAt
    ) {
        this.predictionId = predictionId;
        this.userId = userId;
        this.raceId = raceId;
        this.predictedWinnerEntryId = predictedWinnerEntryId;
        this.predictedSecondEntryId = predictedSecondEntryId;
        this.predictedThirdEntryId = predictedThirdEntryId;
        this.isCorrect = isCorrect;
        this.rewardAmount = rewardAmount;
        this.rewardPaid = rewardPaid;
        this.createdAt = createdAt;
    }

    public Integer getPredictionId() { return predictionId; }
    public Integer getUserId() { return userId; }
    public Integer getRaceId() { return raceId; }
    public Integer getPredictedWinnerEntryId() { return predictedWinnerEntryId; }
    public Integer getPredictedSecondEntryId() { return predictedSecondEntryId; }
    public Integer getPredictedThirdEntryId() { return predictedThirdEntryId; }
    public Boolean getIsCorrect() { return isCorrect; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public Boolean getRewardPaid() { return rewardPaid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
