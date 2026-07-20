package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.LeaderboardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/api/tournaments/{tournamentId}/leaderboard/jockeys")
    public ApiResponse<?> getTournamentJockeyLeaderboard(@PathVariable Integer tournamentId) {
        return ApiResponse.success(
                200,
                "Tournament jockey leaderboard loaded successfully",
                leaderboardService.getTournamentJockeyLeaderboard(tournamentId)
        );
    }

    @GetMapping("/api/tournaments/{tournamentId}/leaderboard/horses")
    public ApiResponse<?> getTournamentHorseLeaderboard(@PathVariable Integer tournamentId) {
        return ApiResponse.success(
                200,
                "Tournament horse leaderboard loaded successfully",
                leaderboardService.getTournamentHorseLeaderboard(tournamentId)
        );
    }

    @GetMapping("/api/leaderboard/jockeys")
    public ApiResponse<?> getGlobalJockeyLeaderboard() {
        return ApiResponse.success(
                200,
                "System jockey leaderboard loaded successfully",
                leaderboardService.getGlobalJockeyLeaderboard()
        );
    }

    @GetMapping("/api/leaderboard/horses")
    public ApiResponse<?> getGlobalHorseLeaderboard() {
        return ApiResponse.success(
                200,
                "System horse leaderboard loaded successfully",
                leaderboardService.getGlobalHorseLeaderboard()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
