package com.horseracing.controller;

import com.horseracing.dto.ViolationRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.ViolationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@CrossOrigin("*")
public class ViolationController {

    private final ViolationService violationService;

    public ViolationController(ViolationService violationService) {
        this.violationService = violationService;
    }

    @GetMapping("/api/races/{raceId}/violations")
    public ApiResponse<?> getViolations(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Lấy danh sách vi phạm thành công",
                violationService.getViolationsByRace(raceId)
        );
    }

    @PostMapping("/api/races/{raceId}/violations")
    public ApiResponse<?> createViolation(
            @PathVariable Integer raceId,
            @RequestBody ViolationRequest request
    ) {
        return ApiResponse.success(
                201,
                "Tạo vi phạm thành công",
                violationService.createViolation(raceId, request)
        );
    }

    @PutMapping("/api/violations/{violationId}")
    public ApiResponse<?> updateViolation(
            @PathVariable Integer violationId,
            @RequestBody ViolationRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cập nhật vi phạm thành công",
                violationService.updateViolation(violationId, request)
        );
    }

    @DeleteMapping("/api/violations/{violationId}")
    public ApiResponse<?> deleteViolation(@PathVariable Integer violationId) {
        violationService.deleteViolation(violationId);
        return ApiResponse.success(200, "Xóa vi phạm thành công", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
