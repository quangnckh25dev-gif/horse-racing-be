package com.horseracing.controller;

import com.horseracing.dto.BetRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.BettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class BettingController {

    private final BettingService bettingService;

    public BettingController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @GetMapping("/api/races/{raceId}/bet-options")
    public ApiResponse<?> getBetOptions(@PathVariable Integer raceId) {
        return ApiResponse.success(200, "Lay lua chon dat cuoc thanh cong", bettingService.getBetOptions(raceId));
    }

    @GetMapping("/api/races/{raceId}/bets")
    public ApiResponse<?> getMineByRace(@PathVariable Integer raceId, HttpServletRequest request) {
        return ApiResponse.success(200, "Lay ve cuoc cua toi thanh cong", bettingService.getMineByRace(raceId, request));
    }

    @PostMapping("/api/races/{raceId}/bets")
    public ApiResponse<?> placeBet(@PathVariable Integer raceId, @RequestBody BetRequest body, HttpServletRequest request) {
        return ApiResponse.success(201, "Dat cuoc thanh cong", bettingService.placeBet(raceId, body, request));
    }

    @GetMapping("/api/bets/history")
    public ApiResponse<?> getMyHistory(HttpServletRequest request) {
        return ApiResponse.success(200, "Lay lich su dat cuoc thanh cong", bettingService.getMyHistory(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
