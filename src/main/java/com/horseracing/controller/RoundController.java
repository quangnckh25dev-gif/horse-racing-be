package com.horseracing.controller;

import com.horseracing.dto.RoundRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.RoundService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer")
public class RoundController {
    //của buiquangann
    private final RoundService roundService;
    private final CurrentUserService currentUserService;

    public RoundController(RoundService roundService, CurrentUserService currentUserService) {
        this.roundService = roundService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/tournaments/{tournamentId}/rounds")
    public ApiResponse<?> create(@PathVariable Integer tournamentId, @RequestBody RoundRequest request,
                                 HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(201, "Tao round thanh cong",
                roundService.createRound(tournamentId, request, organizer));
    }

    @PutMapping("/rounds/{roundId}")
    public ApiResponse<?> update(@PathVariable Integer roundId, @RequestBody RoundRequest request,
                                 HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Cap nhat round thanh cong",
                roundService.updateRound(roundId, request, organizer));
    }

    @DeleteMapping("/rounds/{roundId}")
    public ApiResponse<?> delete(@PathVariable Integer roundId, HttpServletRequest httpRequest) {
        roundService.deleteRound(roundId, currentUserService.getCurrentUser(httpRequest));
        return ApiResponse.success(200, "Xoa round thanh cong", null);
    }
}
