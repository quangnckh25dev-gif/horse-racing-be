package com.horseracing.controller;

import com.horseracing.dto.RaceStatusRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RaceService;
import com.horseracing.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/races")
@CrossOrigin("*")
public class RaceController {

    private final RaceService raceService;
    private final CurrentUserService currentUserService;

    public RaceController(RaceService raceService, CurrentUserService currentUserService) {
        this.raceService = raceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<?> getRaces(
            @RequestParam(required = false) Integer tournamentId,
            @RequestParam(required = false) Integer roundId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(
                200,
                "Lay danh sach race thanh cong",
                raceService.getRaces(tournamentId, roundId, status)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getRaceDetail(@PathVariable Integer id) {
        return ApiResponse.success(
                200,
                "Lay chi tiet race thanh cong",
                raceService.getRaceDetail(id)
        );
    }

    @GetMapping("/{id}/schedule")
    public ApiResponse<?> getRaceSchedule(@PathVariable Integer id) {
        return ApiResponse.success(
                200,
                "Lay lich thi dau race thanh cong",
                raceService.getSchedule(id)
        );
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateRaceStatus(
            @PathVariable Integer id,
            @RequestBody RaceStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat status race thanh cong",
                raceService.updateStatus(id, request.getStatus(), currentUserService.getCurrentUser(httpRequest))
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
