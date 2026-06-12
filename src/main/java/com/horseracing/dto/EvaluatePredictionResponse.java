package com.horseracing.dto;

public class EvaluatePredictionResponse {
    private final Integer raceId;
    private final int totalPredictions;
    private final int correctPredictions;

    public EvaluatePredictionResponse(Integer raceId, int totalPredictions, int correctPredictions) {
        this.raceId = raceId;
        this.totalPredictions = totalPredictions;
        this.correctPredictions = correctPredictions;
    }

    public Integer getRaceId() { return raceId; }
    public int getTotalPredictions() { return totalPredictions; }
    public int getCorrectPredictions() { return correctPredictions; }
}
