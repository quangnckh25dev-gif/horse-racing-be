package com.horseracing.controller;

import com.horseracing.dto.RaceMinuteRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.RaceMinuteService;
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
public class RaceMinuteController {

    private final RaceMinuteService raceMinuteService;
    private final CurrentUserService currentUserService;

    public RaceMinuteController(RaceMinuteService raceMinuteService, CurrentUserService currentUserService) {
        this.raceMinuteService = raceMinuteService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/races/{raceId}/minutes")
    public ApiResponse<?> getMinutes(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Lay bien ban cuoc dua thanh cong",
                raceMinuteService.getMinutesByRace(raceId)
        );
    }

    @PostMapping("/api/races/{raceId}/minutes")
    public ApiResponse<?> createMinutes(
            @PathVariable Integer raceId,
            @RequestBody RaceMinuteRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                201,
                "Tao bien ban cuoc dua thanh cong",
                raceMinuteService.createMinutes(raceId, request, currentUser)
        );
    }

    @PutMapping("/api/races/{raceId}/minutes")
    public ApiResponse<?> updateMinutes(
            @PathVariable Integer raceId,
            @RequestBody RaceMinuteRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Cap nhat bien ban cuoc dua thanh cong",
                raceMinuteService.updateMinutes(raceId, request, currentUser)
        );
    }

    @PostMapping("/api/races/{raceId}/minutes/send")
    public ApiResponse<?> sendMinutesToOwners(@PathVariable Integer raceId, HttpServletRequest httpRequest) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Gui bien ban cho owner thanh cong",
                raceMinuteService.sendMinutesToOwners(raceId, currentUser)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
