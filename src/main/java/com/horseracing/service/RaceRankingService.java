package com.horseracing.service;

import com.horseracing.entity.RaceResult;
import com.horseracing.repository.RaceResultRepository;
import com.horseracing.repository.ViolationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class RaceRankingService {

    private final RaceResultRepository raceResultRepository;
    private final ViolationRepository violationRepository;

    public RaceRankingService(RaceResultRepository raceResultRepository,
                              ViolationRepository violationRepository) {
        this.raceResultRepository = raceResultRepository;
        this.violationRepository = violationRepository;
    }

    @Transactional
    public void recalculateRace(Integer raceId) {
        List<RaceResult> results = raceResultRepository.findByRaceId(raceId);
        if (results.isEmpty()) {
            return;
        }

        for (RaceResult result : results) {
            BigDecimal penalty = violationRepository.sumPenaltySecondsByRaceAndEntry(raceId, result.getEntryId());
            boolean dqByViolation = violationRepository.countDqByRaceAndEntry(raceId, result.getEntryId()) > 0;

            result.setPenaltyTime(penalty == null ? BigDecimal.ZERO : penalty);
            result.setDq(dqByViolation);
            if (Boolean.TRUE.equals(result.getDnf()) || dqByViolation || result.getFinishTime() == null) {
                result.setFinalTime(null);
                result.setFinishPosition(null);
            } else {
                result.setFinalTime(result.getFinishTime().add(result.getPenaltyTime()));
            }
        }

        List<RaceResult> ranked = results.stream()
                .filter(result -> !Boolean.TRUE.equals(result.getDnf())
                        && !Boolean.TRUE.equals(result.getDq())
                        && result.getFinalTime() != null)
                .sorted(Comparator.comparing(RaceResult::getFinalTime)
                        .thenComparing(RaceResult::getResultId))
                .toList();

        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index).setFinishPosition(index + 1);
        }

        raceResultRepository.saveAll(results);
    }
}
