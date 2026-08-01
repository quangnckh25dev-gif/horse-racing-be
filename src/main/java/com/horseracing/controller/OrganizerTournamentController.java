package com.horseracing.controller;

import com.horseracing.dto.TournamentRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.TournamentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/organizer/tournaments")
public class OrganizerTournamentController {

    // Organizer tournament management APIs.
    private final TournamentService tournamentService;
    private final CurrentUserService currentUserService;

    public OrganizerTournamentController(TournamentService tournamentService,
                                         CurrentUserService currentUserService) {
        this.tournamentService = tournamentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<?> getMyTournaments(HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Organizer tournaments loaded successfully",
                tournamentService.getMyTournaments(organizer));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getMyTournament(@PathVariable Integer id, HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Tournament detail loaded successfully",
                tournamentService.getOwnedTournamentDetail(id, organizer));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createTournament(@Valid @RequestBody TournamentRequest request,
                                           HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(201, "Tournament created successfully",
                tournamentService.createTournament(request, organizer));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> updateTournament(@PathVariable Integer id,
                                           @Valid @RequestBody TournamentRequest request,
                                           HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Tournament updated successfully",
                tournamentService.updateTournament(id, request, organizer));
    }

    @PutMapping("/{id}/submit")
    public ApiResponse<?> submitTournament(@PathVariable Integer id, HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(200, "Tournament submission is no longer required",
                tournamentService.submitTournament(id, organizer));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
