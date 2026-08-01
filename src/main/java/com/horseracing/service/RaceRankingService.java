package com.horseracing.service;

import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.RaceResult;
import com.horseracing.repository.RaceEntryRepository;
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
    private final RaceEntryRepository raceEntryRepository;

    public RaceRankingService(RaceResultRepository raceResultRepository,
                              ViolationRepository violationRepository,
                              RaceEntryRepository raceEntryRepository) {
        this.raceResultRepository = raceResultRepository;
        this.violationRepository = violationRepository;
        this.raceEntryRepository = raceEntryRepository;
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
            boolean preRaceRejected = isPreRaceRejected(result.getEntryId());

            result.setPenaltyTime(penalty == null ? BigDecimal.ZERO : penalty);
            result.setDq(dqByViolation);
            if (Boolean.TRUE.equals(result.getDnf()) || dqByViolation || preRaceRejected || result.getFinishTime() == null) {
                result.setFinalTime(null);
                result.setFinishPosition(null);
            } else {
                result.setFinalTime(result.getFinishTime().add(result.getPenaltyTime()));
            }
        }

        List<RaceResult> ranked = results.stream()
                .filter(result -> !Boolean.TRUE.equals(result.getDnf())
                        && !Boolean.TRUE.equals(result.getDq())
                        && !isPreRaceRejected(result.getEntryId())
                        && result.getFinalTime() != null)
                .sorted(Comparator.comparing(RaceResult::getFinalTime)
                        .thenComparing(RaceResult::getResultId))
                .toList();

        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index).setFinishPosition(index + 1);
        }

        raceResultRepository.saveAll(results);
    }

    private boolean isPreRaceRejected(Integer entryId) {
        return raceEntryRepository.findById(entryId)
                .map(RaceEntry::getRegistrationStatus)
                .map(status -> "PreRaceRejected".equalsIgnoreCase(status))
                .orElse(false);
    }
}
