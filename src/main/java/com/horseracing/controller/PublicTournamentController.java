package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@CrossOrigin("*")
public class PublicTournamentController {

    private final TournamentService tournamentService;

    public PublicTournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public ApiResponse<?> getTournaments() {
        return ApiResponse.success(
                200,
                "Lay danh sach tournament thanh cong",
                tournamentService.getPublicTournaments()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getTournamentDetail(@PathVariable Integer id) {
        return ApiResponse.success(
                200,
                "Lay chi tiet tournament thanh cong",
                tournamentService.getTournamentDetail(id)
        );
    }

    @GetMapping("/{id}/rounds")
    public ApiResponse<?> getTournamentRounds(@PathVariable Integer id) {
        return ApiResponse.success(
                200,
                "Lay danh sach round cua tournament thanh cong",
                tournamentService.getTournamentRounds(id)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
