package com.horseracing.controller;

import com.horseracing.dto.TournamentRequest;
import com.horseracing.dto.TournamentStatusRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments")
@CrossOrigin("*")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public ApiResponse<?> getTournaments() {
        return ApiResponse.success(
                200,
                "Lay danh sach tournament thanh cong",
                tournamentService.getAllTournaments()
        );
    }

    @PostMapping
    public ApiResponse<?> createTournament(@RequestBody TournamentRequest request) {
        return ApiResponse.success(
                201,
                "Tao tournament thanh cong",
                tournamentService.createTournament(request)
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

    @PutMapping("/{id}")
    public ApiResponse<?> updateTournament(
            @PathVariable Integer id,
            @RequestBody TournamentRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat tournament thanh cong",
                tournamentService.updateTournament(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteTournament(@PathVariable Integer id) {
        tournamentService.deleteTournament(id);
        return ApiResponse.success(200, "Xoa tournament thanh cong", null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> updateTournamentStatus(
            @PathVariable Integer id,
            @RequestBody TournamentStatusRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat status tournament thanh cong",
                tournamentService.updateStatus(id, request.getStatus())
        );
    }

    @GetMapping("/{id}/status-transitions")
    public ApiResponse<?> getTournamentStatusTransitions(@PathVariable Integer id) {
        return ApiResponse.success(
                200,
                "Lấy danh sách trạng thái kế tiếp thành công",
                tournamentService.getStatusTransitions(id)
        );
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> patchTournamentStatus(
            @PathVariable Integer id,
            @RequestBody TournamentStatusRequest request
    ) {
        return updateTournamentStatus(id, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
