package com.horseracing.controller;

import com.horseracing.dto.ViolationRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.ViolationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class ViolationController {

    private final ViolationService violationService;
    private final CurrentUserService currentUserService;

    public ViolationController(ViolationService violationService, CurrentUserService currentUserService) {
        this.violationService = violationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/violations/options")
    public ApiResponse<?> getViolationOptions() {
        return ApiResponse.success(200, "Lay danh sach loai vi pham thanh cong", violationService.getViolationOptions());
    }

    @GetMapping("/api/races/{raceId}/violations")
    public ApiResponse<?> getViolations(@PathVariable Integer raceId) {
        return ApiResponse.success(200, "Lay danh sach vi pham thanh cong", violationService.getViolationsByRace(raceId));
    }

    @PostMapping("/api/races/{raceId}/violations")
    public ApiResponse<?> createViolation(@PathVariable Integer raceId, @RequestBody ViolationRequest request,
                                          HttpServletRequest httpRequest) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(201, "Tao vi pham thanh cong", violationService.createViolation(raceId, request, currentUser));
    }

    @PutMapping("/api/violations/{violationId}")
    public ApiResponse<?> updateViolation(@PathVariable Integer violationId, @RequestBody ViolationRequest request,
                                          HttpServletRequest httpRequest) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Cap nhat vi pham thanh cong", violationService.updateViolation(violationId, request, currentUser));
    }

    @DeleteMapping("/api/violations/{violationId}")
    public ApiResponse<?> deleteViolation(@PathVariable Integer violationId, HttpServletRequest httpRequest) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        violationService.deleteViolation(violationId, currentUser);
        return ApiResponse.success(200, "Xoa vi pham thanh cong", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
