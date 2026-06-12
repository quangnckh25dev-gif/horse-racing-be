package com.horseracing.controller;

import com.horseracing.dto.PredictionRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.PredictionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/api/races/{id}/predictions")
    public ApiResponse<?> getMineByRace(@PathVariable Integer id, HttpServletRequest request) {
        return ApiResponse.success(
                200,
                "Lay du doan cua toi thanh cong",
                predictionService.getMineByRace(id, request)
        );
    }

    @PostMapping("/api/races/{id}/predictions")
    public ApiResponse<?> createPrediction(
            @PathVariable Integer id,
            @RequestBody PredictionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                201,
                "Tao du doan thanh cong",
                predictionService.createPrediction(id, body, request)
        );
    }

    @PutMapping("/api/races/{id}/predictions")
    public ApiResponse<?> updatePrediction(
            @PathVariable Integer id,
            @RequestBody PredictionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat du doan thanh cong",
                predictionService.updatePrediction(id, body, request)
        );
    }

    @GetMapping("/api/predictions/history")
    public ApiResponse<?> getMyHistory(HttpServletRequest request) {
        return ApiResponse.success(
                200,
                "Lay lich su du doan thanh cong",
                predictionService.getMyHistory(request)
        );
    }

    @PostMapping("/api/races/{id}/predictions/evaluate")
    public ApiResponse<?> evaluatePredictions(@PathVariable Integer id, HttpServletRequest request) {
        return ApiResponse.success(
                200,
                "Xet du doan thanh cong",
                predictionService.evaluatePredictions(id, request)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleForbidden(SecurityException ex) {
        return ApiResponse.error(403, ex.getMessage());
    }
}
