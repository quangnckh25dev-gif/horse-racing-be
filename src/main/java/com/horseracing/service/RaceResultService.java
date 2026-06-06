package com.horseracing.service;

import com.horseracing.dto.RaceResultRequest;
import com.horseracing.dto.RaceResultResponse;
import com.horseracing.entity.RaceResult;
import com.horseracing.repository.RaceResultRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RaceResultService {

    private final RaceResultRepository raceResultRepository;

    public RaceResultService(RaceResultRepository raceResultRepository) {
        this.raceResultRepository = raceResultRepository;
    }

    public List<RaceResultResponse> getResultsByRace(Integer raceId) {
        ensureRaceExists(raceId);
        return raceResultRepository.findByRaceId(raceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceResultResponse createResult(Integer raceId, RaceResultRequest request) {
        ensureRaceExists(raceId);
        ensureEntryBelongsToRace(raceId, request.getEntryId());

        raceResultRepository.findByRaceIdAndEntryId(raceId, request.getEntryId())
                .ifPresent(result -> {
                    throw new IllegalArgumentException("Entry này đã có kết quả trong race");
                });

        RaceResult result = new RaceResult();
        result.setRaceId(raceId);
        result.setEntryId(request.getEntryId());
        result.setFinishPosition(resolvePosition(request));
        result.setFinishTime(parseFinishTime(request.getFinishTime()));
        result.setPrizeWon(resolvePrizeWon(request));
        result.setDnf(Boolean.TRUE.equals(request.getDnf()));
        result.setDq(Boolean.TRUE.equals(request.getDq()));
        result.setConfirmedByRef(request.getConfirmedByRef());
        result.setCreatedAt(LocalDateTime.now());

        return toResponse(raceResultRepository.save(result));
    }

    public RaceResultResponse updateResult(Integer raceId, Integer resultId, RaceResultRequest request) {
        ensureRaceExists(raceId);

        RaceResult result = raceResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả"));

        if (!result.getRaceId().equals(raceId)) {
            throw new IllegalArgumentException("Kết quả không thuộc race này");
        }

        ensureEntryBelongsToRace(raceId, request.getEntryId());

        result.setEntryId(request.getEntryId());
        result.setFinishPosition(resolvePosition(request));
        result.setFinishTime(parseFinishTime(request.getFinishTime()));
        result.setPrizeWon(resolvePrizeWon(request));
        result.setDnf(Boolean.TRUE.equals(request.getDnf()));
        result.setDq(Boolean.TRUE.equals(request.getDq()));
        result.setConfirmedByRef(request.getConfirmedByRef());

        return toResponse(raceResultRepository.save(result));
    }

    public List<RaceResultResponse> publishResults(Integer raceId) {
        ensureRaceExists(raceId);

        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);
        LocalDateTime now = LocalDateTime.now();

        results.forEach(result -> result.setConfirmedAt(now));

        return raceResultRepository.saveAll(results)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureRaceExists(Integer raceId) {
        if (raceId == null || raceResultRepository.countRaceById(raceId) == 0) {
            throw new IllegalArgumentException("Không tìm thấy race");
        }
    }

    private void ensureEntryBelongsToRace(Integer raceId, Integer entryId) {
        if (entryId == null || raceResultRepository.countEntryInRace(raceId, entryId) == 0) {
            throw new IllegalArgumentException("Entry không thuộc race này");
        }
    }

    private Integer resolvePosition(RaceResultRequest request) {
        return request.getPosition() != null ? request.getPosition() : request.getFinishPosition();
    }

    private BigDecimal resolvePrizeWon(RaceResultRequest request) {
        if (request.getPrizeWon() != null) {
            return request.getPrizeWon();
        }

        if (request.getPoint() != null) {
            return BigDecimal.valueOf(request.getPoint());
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal parseFinishTime(String finishTime) {
        if (finishTime == null || finishTime.isBlank()) {
            return null;
        }

        String[] parts = finishTime.split(":");
        try {
            if (parts.length == 3) {
                BigDecimal hours = new BigDecimal(parts[0]).multiply(BigDecimal.valueOf(3600));
                BigDecimal minutes = new BigDecimal(parts[1]).multiply(BigDecimal.valueOf(60));
                BigDecimal seconds = new BigDecimal(parts[2]);
                return hours.add(minutes).add(seconds).setScale(3, RoundingMode.HALF_UP);
            }

            return new BigDecimal(finishTime).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("finishTime phải có dạng HH:mm:ss.SSS hoặc số giây");
        }
    }

    private RaceResultResponse toResponse(RaceResult result) {
        return RaceResultResponse.builder()
                .resultId(result.getResultId())
                .raceId(result.getRaceId())
                .entryId(result.getEntryId())
                .position(result.getFinishPosition())
                .finishTime(formatFinishTime(result.getFinishTime()))
                .point(calculatePoint(result.getFinishPosition()))
                .prizeWon(result.getPrizeWon())
                .dnf(result.getDnf())
                .dq(result.getDq())
                .confirmedByRef(result.getConfirmedByRef())
                .confirmedAt(result.getConfirmedAt())
                .published(result.getConfirmedAt() != null)
                .createdAt(result.getCreatedAt())
                .build();
    }

    private Integer calculatePoint(Integer position) {
        if (position == null) {
            return 0;
        }

        return switch (position) {
            case 1 -> 10;
            case 2 -> 7;
            case 3 -> 5;
            default -> 0;
        };
    }

    private String formatFinishTime(BigDecimal finishTime) {
        if (finishTime == null) {
            return null;
        }

        BigDecimal scaled = finishTime.setScale(3, RoundingMode.HALF_UP);
        int totalSeconds = scaled.intValue();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        BigDecimal seconds = scaled.subtract(BigDecimal.valueOf(hours * 3600L + minutes * 60L));

        return "%02d:%02d:%06.3f".formatted(hours, minutes, seconds);
    }
}
