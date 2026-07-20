package com.horseracing.controller;

import com.horseracing.dto.TournamentStatusRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.TournamentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final CurrentUserService currentUserService;

    public TournamentController(TournamentService tournamentService, CurrentUserService currentUserService) {
        this.tournamentService = tournamentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<?> getTournaments(@RequestParam(required = false) String status,
                                         HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ApiResponse.success(200, "Tournaments loaded successfully",
                tournamentService.getAdminTournaments(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getTournamentDetail(@PathVariable Integer id, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ApiResponse.success(200, "Tournament detail loaded successfully",
                tournamentService.getTournamentDetail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> reviewTournament(@PathVariable Integer id,
                                           @RequestBody TournamentStatusRequest request,
                                           HttpServletRequest httpRequest) {
        User admin = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Tournament status updated successfully",
                tournamentService.reviewTournament(id, request.getStatus(), request.getReason(), admin));
    }

    private void requireAdmin(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        if (!currentUserService.isAdmin(user)) {
            throw new IllegalArgumentException("Chi Admin moi co quyen truy cap");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
