package com.horseracing.controller;

import com.horseracing.dto.RaceResultRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RaceResultService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/races/{raceId}/results")
@CrossOrigin("*")
public class RaceResultController {

    private final RaceResultService raceResultService;

    public RaceResultController(RaceResultService raceResultService) {
        this.raceResultService = raceResultService;
    }

    @GetMapping
    public ApiResponse<?> getResults(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Lấy kết quả cuộc đua thành công",
                raceResultService.getResultsByRace(raceId)
        );
    }

    @PostMapping
    public ApiResponse<?> createResult(
            @PathVariable Integer raceId,
            @RequestBody RaceResultRequest request
    ) {
        return ApiResponse.success(
                201,
                "Tạo kết quả cuộc đua thành công",
                raceResultService.createResult(raceId, request)
        );
    }

    @PutMapping("/{resultId}")
    public ApiResponse<?> updateResult(
            @PathVariable Integer raceId,
            @PathVariable Integer resultId,
            @RequestBody RaceResultRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cập nhật kết quả cuộc đua thành công",
                raceResultService.updateResult(raceId, resultId, request)
        );
    }

    @PostMapping("/publish")
    public ApiResponse<?> publishResults(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Công bố kết quả cuộc đua thành công",
                raceResultService.publishResults(raceId)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
