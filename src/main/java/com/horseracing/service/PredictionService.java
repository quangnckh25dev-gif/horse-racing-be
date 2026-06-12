package com.horseracing.service;

import com.horseracing.dto.EvaluatePredictionResponse;
import com.horseracing.dto.PredictionRequest;
import com.horseracing.dto.PredictionResponse;
import com.horseracing.entity.Prediction;
import com.horseracing.entity.Race;
import com.horseracing.entity.RaceEntry;
import com.horseracing.entity.User;
import com.horseracing.repository.PredictionRepository;
import com.horseracing.repository.RaceEntryRepository;
import com.horseracing.repository.RaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public PredictionService(
            PredictionRepository predictionRepository,
            RaceRepository raceRepository,
            RaceEntryRepository raceEntryRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.predictionRepository = predictionRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public PredictionResponse getMineByRace(Integer raceId, HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        ensureRaceExists(raceId);
        Prediction prediction = predictionRepository.findByUserIdAndRaceId(user.getUserId(), raceId)
                .orElseThrow(() -> new IllegalArgumentException("Chua co du doan cho race nay"));
        return toResponse(prediction);
    }

    public List<PredictionResponse> getMyHistory(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return predictionRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PredictionResponse createPrediction(Integer raceId, PredictionRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Race race = ensureRaceOpenForPrediction(raceId);
        validatePredictionRequest(race.getRaceId(), request);

        if (predictionRepository.findByUserIdAndRaceId(user.getUserId(), raceId).isPresent()) {
            throw new IllegalArgumentException("Ban da du doan race nay, hay dung API cap nhat");
        }

        Prediction prediction = new Prediction();
        prediction.setUserId(user.getUserId());
        prediction.setRaceId(raceId);
        applyRequest(prediction, request);
        Prediction saved = predictionRepository.save(prediction);

        auditLogService.log(
                user.getUserId(),
                "CREATE_PREDICTION",
                "Predictions",
                saved.getPredictionId(),
                null,
                formatPrediction(saved),
                auditLogService.getClientIp(httpRequest)
        );

        return toResponse(saved);
    }

    public PredictionResponse updatePrediction(Integer raceId, PredictionRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        Race race = ensureRaceOpenForPrediction(raceId);
        validatePredictionRequest(race.getRaceId(), request);

        Prediction prediction = predictionRepository.findByUserIdAndRaceId(user.getUserId(), raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay du doan de cap nhat"));

        String oldValue = formatPrediction(prediction);
        applyRequest(prediction, request);
        prediction.setIsCorrect(null);
        prediction.setRewardAmount(BigDecimal.ZERO);
        prediction.setRewardPaid(false);
        Prediction saved = predictionRepository.save(prediction);

        auditLogService.log(
                user.getUserId(),
                "UPDATE_PREDICTION",
                "Predictions",
                saved.getPredictionId(),
                oldValue,
                formatPrediction(saved),
                auditLogService.getClientIp(httpRequest)
        );

        return toResponse(saved);
    }

    @Transactional
    public EvaluatePredictionResponse evaluatePredictions(Integer raceId, HttpServletRequest request) {
        User admin = currentUserService.getCurrentUser(request);
        if (!currentUserService.isAdmin(admin)) {
            throw new SecurityException("Chi admin moi co quyen xet du doan");
        }

        ensureRaceExists(raceId);
        entityManager.createNativeQuery("EXEC sp_EvaluatePredictions @RaceID = :raceId")
                .setParameter("raceId", raceId)
                .executeUpdate();

        List<Prediction> predictions = predictionRepository.findByRaceId(raceId);
        int correct = (int) predictions.stream()
                .filter(prediction -> Boolean.TRUE.equals(prediction.getIsCorrect()))
                .count();

        auditLogService.log(
                admin.getUserId(),
                "EVALUATE_PREDICTIONS",
                "Predictions",
                raceId,
                null,
                "total=" + predictions.size() + ", correct=" + correct,
                auditLogService.getClientIp(request)
        );

        return new EvaluatePredictionResponse(raceId, predictions.size(), correct);
    }

    private Race ensureRaceOpenForPrediction(Integer raceId) {
        Race race = ensureRaceExists(raceId);
        if (race.getRaceDate() != null && !race.getRaceDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Race da bat dau, khong the du doan");
        }
        if (race.getStatus() != null
                && ("Running".equalsIgnoreCase(race.getStatus()) || "Finished".equalsIgnoreCase(race.getStatus()))) {
            throw new IllegalArgumentException("Race khong con mo du doan");
        }
        return race;
    }

    private Race ensureRaceExists(Integer raceId) {
        if (raceId == null) {
            throw new IllegalArgumentException("raceId khong duoc de trong");
        }
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race"));
    }

    private void validatePredictionRequest(Integer raceId, PredictionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu du doan khong hop le");
        }
        if (request.getPredictedWinnerEntryId() == null) {
            throw new IllegalArgumentException("predictedWinnerEntryId khong duoc de trong");
        }

        Set<Integer> uniqueEntries = new HashSet<>();
        validateEntryInRace(raceId, request.getPredictedWinnerEntryId(), uniqueEntries);
        validateEntryInRace(raceId, request.getPredictedSecondEntryId(), uniqueEntries);
        validateEntryInRace(raceId, request.getPredictedThirdEntryId(), uniqueEntries);
    }

    private void validateEntryInRace(Integer raceId, Integer entryId, Set<Integer> uniqueEntries) {
        if (entryId == null) {
            return;
        }
        if (!uniqueEntries.add(entryId)) {
            throw new IllegalArgumentException("Khong duoc chon trung entry trong mot du doan");
        }
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay race entry"));
        if (!raceId.equals(entry.getRaceId())) {
            throw new IllegalArgumentException("Race entry khong thuoc race nay");
        }
    }

    private void applyRequest(Prediction prediction, PredictionRequest request) {
        prediction.setPredictedFirst(request.getPredictedWinnerEntryId());
        prediction.setPredictedSecond(request.getPredictedSecondEntryId());
        prediction.setPredictedThird(request.getPredictedThirdEntryId());
    }

    private String formatPrediction(Prediction prediction) {
        return "first=" + prediction.getPredictedFirst()
                + ", second=" + prediction.getPredictedSecond()
                + ", third=" + prediction.getPredictedThird();
    }

    private PredictionResponse toResponse(Prediction prediction) {
        return new PredictionResponse(
                prediction.getPredictionId(),
                prediction.getUserId(),
                prediction.getRaceId(),
                prediction.getPredictedFirst(),
                prediction.getPredictedSecond(),
                prediction.getPredictedThird(),
                prediction.getIsCorrect(),
                prediction.getRewardAmount(),
                prediction.getRewardPaid(),
                prediction.getCreatedAt()
        );
    }
}
