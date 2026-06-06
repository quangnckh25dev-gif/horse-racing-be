package com.horseracing.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class RaceResultResponse {

    private Integer resultId;

    private Integer raceId;

    private Integer entryId;

    private Integer position;

    private String finishTime;

    private Integer point;

    private BigDecimal prizeWon;

    private Boolean dnf;

    private Boolean dq;

    private Integer confirmedByRef;

    private LocalDateTime confirmedAt;

    private Boolean published;

    private LocalDateTime createdAt;

    public RaceResultResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RaceResultResponse response = new RaceResultResponse();

        public Builder resultId(Integer resultId) {
            response.resultId = resultId;
            return this;
        }

        public Builder raceId(Integer raceId) {
            response.raceId = raceId;
            return this;
        }

        public Builder entryId(Integer entryId) {
            response.entryId = entryId;
            return this;
        }

        public Builder position(Integer position) {
            response.position = position;
            return this;
        }

        public Builder finishTime(String finishTime) {
            response.finishTime = finishTime;
            return this;
        }

        public Builder point(Integer point) {
            response.point = point;
            return this;
        }

        public Builder prizeWon(BigDecimal prizeWon) {
            response.prizeWon = prizeWon;
            return this;
        }

        public Builder dnf(Boolean dnf) {
            response.dnf = dnf;
            return this;
        }

        public Builder dq(Boolean dq) {
            response.dq = dq;
            return this;
        }

        public Builder confirmedByRef(Integer confirmedByRef) {
            response.confirmedByRef = confirmedByRef;
            return this;
        }

        public Builder confirmedAt(LocalDateTime confirmedAt) {
            response.confirmedAt = confirmedAt;
            return this;
        }

        public Builder published(Boolean published) {
            response.published = published;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            response.createdAt = createdAt;
            return this;
        }

        public RaceResultResponse build() {
            return response;
        }
    }
}
