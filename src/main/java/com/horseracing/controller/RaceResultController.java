package com.horseracing.controller;

import com.horseracing.dto.RaceResultRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.RaceResultService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CurrentUserService currentUserService;

    public RaceResultController(RaceResultService raceResultService, CurrentUserService currentUserService) {
        this.raceResultService = raceResultService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<?> getResults(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Race results loaded successfully",
                raceResultService.getResultsByRace(raceId)
        );
    }

    @GetMapping("/published")
    public ApiResponse<?> getPublishedResults(@PathVariable Integer raceId) {
        return ApiResponse.success(
                200,
                "Published race results loaded successfully",
                raceResultService.getPublishedResultsByRace(raceId)
        );
    }

    @PostMapping
    public ApiResponse<?> createResult(
            @PathVariable Integer raceId,
            @RequestBody RaceResultRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                201,
                "Race result created successfully",
                raceResultService.createResult(raceId, request, currentUser)
        );
    }

    @PutMapping("/{resultId}")
    public ApiResponse<?> updateResult(
            @PathVariable Integer raceId,
            @PathVariable Integer resultId,
            @RequestBody RaceResultRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Race result updated successfully",
                raceResultService.updateResult(raceId, resultId, request, currentUser)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
