package com.horseracing.dto;

public class PredictionRequest {
    private Integer predictedWinnerEntryId;
    private Integer predictedSecondEntryId;
    private Integer predictedThirdEntryId;

    public Integer getPredictedWinnerEntryId() {
        return predictedWinnerEntryId;
    }

    public void setPredictedWinnerEntryId(Integer predictedWinnerEntryId) {
        this.predictedWinnerEntryId = predictedWinnerEntryId;
    }

    public Integer getPredictedSecondEntryId() {
        return predictedSecondEntryId;
    }

    public void setPredictedSecondEntryId(Integer predictedSecondEntryId) {
        this.predictedSecondEntryId = predictedSecondEntryId;
    }

    public Integer getPredictedThirdEntryId() {
        return predictedThirdEntryId;
    }

    public void setPredictedThirdEntryId(Integer predictedThirdEntryId) {
        this.predictedThirdEntryId = predictedThirdEntryId;
    }
}
