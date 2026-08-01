package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/admin/dashboard")
    public ApiResponse<?> getDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Dashboard loaded successfully", dashboardService.getAdminDashboard(request));
    }

    @GetMapping("/api/dashboard")
    public ApiResponse<?> getSharedDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Dashboard loaded successfully", dashboardService.getDashboardForCurrentUser(request));
    }

    @GetMapping("/api/dashboard/admin")
    public ApiResponse<?> getAdminDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Admin dashboard loaded successfully", dashboardService.getAdminDashboard(request));
    }

    @GetMapping("/api/dashboard/organizer")
    public ApiResponse<?> getOrganizerDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Organizer dashboard loaded successfully", dashboardService.getOrganizerDashboard(request));
    }

    @GetMapping("/api/dashboard/referee")
    public ApiResponse<?> getRefereeDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Referee dashboard loaded successfully", dashboardService.getRefereeDashboard(request));
    }

    @GetMapping("/api/dashboard/owner")
    public ApiResponse<?> getOwnerDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Owner dashboard loaded successfully", dashboardService.getOwnerDashboard(request));
    }

    @GetMapping("/api/dashboard/jockey")
    public ApiResponse<?> getJockeyDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Jockey dashboard loaded successfully", dashboardService.getJockeyDashboard(request));
    }

    @GetMapping("/api/dashboard/spectator")
    public ApiResponse<?> getSpectatorDashboard(HttpServletRequest request) {
        return ApiResponse.success(200, "Spectator dashboard loaded successfully", dashboardService.getSpectatorDashboard(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
