package com.horseracing.controller;

import com.horseracing.dto.HorseHealthRecordRequest;
import com.horseracing.dto.HorseRequest;
import com.horseracing.dto.HorseStatusRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.HorseService;
import com.horseracing.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class HorseController {

    private final HorseService horseService;
    private final StatsService statsService;

    public HorseController(HorseService horseService, StatsService statsService) {
        this.horseService = horseService;
        this.statsService = statsService;
    }

    @GetMapping("/api/horses")
    public ApiResponse<?> getHorses(HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Lay danh sach horse thanh cong", horseService.getHorses(httpRequest));
    }

    @GetMapping("/api/horses/options")
    public ApiResponse<?> getHorseOptions() {
        return ApiResponse.success(200, "Lấy danh sách tùy chọn ngựa thành công", horseService.getHorseOptions());
    }

    @GetMapping("/api/horses/{horseId}")
    public ApiResponse<?> getHorse(@PathVariable Integer horseId, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Lay chi tiet horse thanh cong", horseService.getHorse(horseId, httpRequest));
    }

    @PostMapping("/api/horses")
    public ApiResponse<?> createHorse(@RequestBody HorseRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(201, "Tao horse thanh cong", horseService.createHorse(request, httpRequest));
    }

    @PutMapping("/api/horses/{horseId}")
    public ApiResponse<?> updateHorse(@PathVariable Integer horseId, @RequestBody HorseRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Cap nhat horse thanh cong", horseService.updateHorse(horseId, request, httpRequest));
    }

    @PatchMapping({"/api/horses/{horseId}/health", "/api/horses/{horseId}/status"})
    public ApiResponse<?> updateHorseStatus(@PathVariable Integer horseId, @RequestBody HorseStatusRequest request, HttpServletRequest httpRequest) {
        //của buiquangann
        return ApiResponse.success(200, "Cap nhat trang thai horse thanh cong", horseService.updateHorseStatus(horseId, request, httpRequest));
    }

    @GetMapping({"/api/horses/{horseId}/health-records", "/api/horses/{horseId}/health"})
    public ApiResponse<?> getHealthHistory(@PathVariable Integer horseId, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Lay lich su suc khoe horse thanh cong", horseService.getHealthHistory(horseId, httpRequest));
    }

    @GetMapping("/api/horses/{horseId}/stats")
    public ApiResponse<?> getHorseStats(@PathVariable Integer horseId) {
        return ApiResponse.success(200, "Lay thong ke horse thanh cong", statsService.getHorseStats(horseId));
    }

    @PostMapping({"/api/horses/{horseId}/health-records", "/api/horses/{horseId}/health"})
    public ApiResponse<?> addHealthRecord(@PathVariable Integer horseId, @RequestBody HorseHealthRecordRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(201, "Them ho so suc khoe horse thanh cong", horseService.addHealthRecord(horseId, request, httpRequest));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
