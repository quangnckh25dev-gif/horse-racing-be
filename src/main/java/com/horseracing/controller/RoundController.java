package com.horseracing.controller;

import com.horseracing.dto.RoundRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RoundService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
public class RoundController {

    private final RoundService roundService;

    public RoundController(RoundService roundService) {
        this.roundService = roundService;
    }

    @PostMapping("/tournaments/{tournamentId}/rounds")
    public ApiResponse<?> createRound(
            @PathVariable Integer tournamentId,
            @RequestBody RoundRequest request
    ) {
        return ApiResponse.success(
                201,
                "Tao round thanh cong",
                roundService.createRound(tournamentId, request)
        );
    }

    @PutMapping("/rounds/{id}")
    public ApiResponse<?> updateRound(
            @PathVariable Integer id,
            @RequestBody RoundRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat round thanh cong",
                roundService.updateRound(id, request)
        );
    }

    @DeleteMapping("/rounds/{id}")
    public ApiResponse<?> deleteRound(@PathVariable Integer id) {
        roundService.deleteRound(id);
        return ApiResponse.success(200, "Xoa round thanh cong", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
