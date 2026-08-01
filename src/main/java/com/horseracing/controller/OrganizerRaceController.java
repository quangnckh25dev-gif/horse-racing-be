package com.horseracing.controller;

import com.horseracing.dto.RaceRefereeRequest;
import com.horseracing.dto.RaceRequest;
import com.horseracing.dto.RaceStatusRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.RaceRefereeService;
import com.horseracing.service.RaceResultService;
import com.horseracing.service.RaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer")
@CrossOrigin("*")
public class OrganizerRaceController {

    // Organizer race management APIs.
    private final RaceService raceService;
    private final RaceRefereeService raceRefereeService;
    private final RaceResultService raceResultService;
    private final CurrentUserService currentUserService;

    public OrganizerRaceController(RaceService raceService,
                                   RaceRefereeService raceRefereeService,
                                   RaceResultService raceResultService,
                                   CurrentUserService currentUserService) {
        this.raceService = raceService;
        this.raceRefereeService = raceRefereeService;
        this.raceResultService = raceResultService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/races")
    public ApiResponse<?> getMyRaces(HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Organizer races loaded successfully",
                raceService.getOrganizerRaces(organizer)
        );
    }

    @PostMapping("/races")
    public ApiResponse<?> createRace(@RequestBody RaceRequest request, HttpServletRequest httpRequest) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                201,
                "Race created successfully",
                raceService.createRace(request, organizer)
        );
    }

    @PutMapping("/races/{id}")
    public ApiResponse<?> updateRace(
            @PathVariable Integer id,
            @RequestBody RaceRequest request,
            HttpServletRequest httpRequest
    ) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Race updated successfully",
                raceService.updateRace(id, request, organizer)
        );
    }

    @PatchMapping("/races/{id}/status")
    public ApiResponse<?> updateRaceStatus(
            @PathVariable Integer id,
            @RequestBody RaceStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        User organizer = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Race status updated successfully",
                raceService.updateStatusByOrganizer(id, request.getStatus(), organizer)
        );
    }

    @DeleteMapping("/races/{id}")
    public ApiResponse<?> deleteRace(@PathVariable Integer id, HttpServletRequest httpRequest) {
        raceService.deleteRace(id, currentUserService.getCurrentUser(httpRequest));
        return ApiResponse.success(200, "Race deleted successfully", null);
    }

    @GetMapping("/referees")
    public ApiResponse<?> getReferees(HttpServletRequest httpRequest) {
        return ApiResponse.success(
                200,
                "Referees loaded successfully",
                raceRefereeService.getReferees(currentUserService.getCurrentUser(httpRequest))
        );
    }

    @GetMapping("/races/{id}/referees")
    public ApiResponse<?> getRaceReferees(@PathVariable Integer id, HttpServletRequest httpRequest) {
        return ApiResponse.success(
                200,
                "Assigned referees loaded successfully",
                raceRefereeService.getRaceReferees(id, currentUserService.getCurrentUser(httpRequest))
        );
    }

    @PostMapping("/races/{id}/referees")
    public ApiResponse<?> assignReferee(
            @PathVariable Integer id,
            @RequestBody RaceRefereeRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                201,
                "Referee assigned successfully",
                raceRefereeService.assignReferee(id, request, currentUserService.getCurrentUser(httpRequest))
        );
    }

    @DeleteMapping("/races/{id}/referees/{refereeId}")
    public ApiResponse<?> removeReferee(
            @PathVariable Integer id,
            @PathVariable Integer refereeId,
            HttpServletRequest httpRequest
    ) {
        raceRefereeService.removeReferee(id, refereeId, currentUserService.getCurrentUser(httpRequest));
        return ApiResponse.success(200, "Referee assignment removed successfully", null);
    }

    @PutMapping("/races/{raceId}/results/approve")
    public ApiResponse<?> approveResults(
            @PathVariable Integer raceId,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Race results approved successfully",
                raceResultService.approveResults(raceId, currentUser)
        );
    }

    @PutMapping("/races/{raceId}/results/reject")
    public ApiResponse<?> rejectResults(
            @PathVariable Integer raceId,
            @RequestBody(required = false) ResultRejectRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Race results rejected successfully",
                raceResultService.rejectResults(raceId, request == null ? null : request.reason(), currentUser)
        );
    }

    @PostMapping("/races/{raceId}/results/publish")
    public ApiResponse<?> publishResults(
            @PathVariable Integer raceId,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser(httpRequest);
        return ApiResponse.success(
                200,
                "Official race results published successfully",
                raceResultService.publishResults(raceId, currentUser)
        );
    }

    public record ResultRejectRequest(String reason) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
