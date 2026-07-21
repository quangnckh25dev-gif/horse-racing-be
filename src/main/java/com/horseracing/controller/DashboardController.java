package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.DashboardService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/admin/dashboard")
    public ApiResponse<?> getDashboard() {
        return ApiResponse.success(200, "Dashboard loaded successfully", dashboardService.getDashboard());
    }

    @GetMapping("/api/dashboard")
    public ApiResponse<?> getSharedDashboard() {
        return ApiResponse.success(200, "Dashboard loaded successfully", dashboardService.getSharedDashboard());
    }
}
