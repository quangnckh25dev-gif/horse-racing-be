package com.horseracing.controller;

import com.horseracing.dto.RaceMinuteRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RaceMinuteService;
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
public class RaceMinuteController {

    private final RaceMinuteService raceMinuteService;

    public RaceMinuteController(RaceMinuteService raceMinuteService) {
        this.raceMinuteService = raceMinuteService;
    }

    @GetMapping("/api/races/{raceId}/minutes")
    public ApiResponse<?> getMinutes(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Lấy biên bản cuộc đua thành công",
                raceMinuteService.getMinutesByRace(raceId)
        );
    }

    @PostMapping("/api/races/{raceId}/minutes")
    public ApiResponse<?> createMinutes(
            @PathVariable Integer raceId,
            @RequestBody RaceMinuteRequest request
    ) {
        return ApiResponse.success(
                201,
                "Tạo biên bản cuộc đua thành công",
                raceMinuteService.createMinutes(raceId, request)
        );
    }

    @PutMapping("/api/races/{raceId}/minutes")
    public ApiResponse<?> updateMinutes(
            @PathVariable Integer raceId,
            @RequestBody RaceMinuteRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cập nhật biên bản cuộc đua thành công",
                raceMinuteService.updateMinutes(raceId, request)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
