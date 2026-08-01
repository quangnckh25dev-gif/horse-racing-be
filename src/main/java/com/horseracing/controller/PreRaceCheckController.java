package com.horseracing.controller;

import com.horseracing.dto.PreRaceCheckRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.PreRaceCheckService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class PreRaceCheckController {

    private final PreRaceCheckService preRaceCheckService;
    private final CurrentUserService currentUserService;

    public PreRaceCheckController(PreRaceCheckService preRaceCheckService,
                                  CurrentUserService currentUserService) {
        this.preRaceCheckService = preRaceCheckService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/races/{raceId}/pre-race-checks")
    public ApiResponse<?> getChecks(@PathVariable Integer raceId, HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(200, "Pre-race checks loaded successfully",
                preRaceCheckService.getChecks(raceId, currentUser));
    }

    @PostMapping("/api/races/{raceId}/pre-race-checks/init")
    public ApiResponse<?> initChecks(@PathVariable Integer raceId, HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(201, "Pre-race checks initialized successfully",
                preRaceCheckService.initChecks(raceId, currentUser));
    }

    @PatchMapping("/api/races/{raceId}/pre-race-checks/{entryId}")
    public ApiResponse<?> updateCheck(@PathVariable Integer raceId,
                                      @PathVariable Integer entryId,
                                      @RequestBody PreRaceCheckRequest body,
                                      HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(200, "Pre-race check updated successfully",
                preRaceCheckService.updateCheck(raceId, entryId, body, currentUser));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
