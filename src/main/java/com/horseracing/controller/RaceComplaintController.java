package com.horseracing.controller;

import com.horseracing.dto.RaceComplaintRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RaceComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class RaceComplaintController {

    private final RaceComplaintService raceComplaintService;

    public RaceComplaintController(RaceComplaintService raceComplaintService) {
        this.raceComplaintService = raceComplaintService;
    }

    @PostMapping("/api/complaints/races")
    public ApiResponse<?> createComplaint(@RequestBody RaceComplaintRequest body, HttpServletRequest request) {
        return ApiResponse.success(201, "Race complaint created successfully",
                raceComplaintService.createComplaint(body, request));
    }

    @GetMapping("/api/complaints/races/mine")
    public ApiResponse<?> getMyComplaints(HttpServletRequest request) {
        return ApiResponse.success(200, "My race complaints loaded successfully",
                raceComplaintService.getMyComplaints(request));
    }

    @GetMapping("/api/referee/complaints/races")
    public ApiResponse<?> getRefereeComplaints(HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaints loaded successfully",
                raceComplaintService.getRefereeComplaints(request));
    }

    @GetMapping("/api/referee/complaints/races/{id}")
    public ApiResponse<?> getRefereeComplaint(@PathVariable Integer id, HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaint detail loaded successfully",
                raceComplaintService.getRefereeComplaint(id, request));
    }

    @PutMapping("/api/referee/complaints/races/{id}/resolve")
    public ApiResponse<?> refereeResolve(@PathVariable Integer id,
                                         @RequestBody(required = false) RaceComplaintRequest body,
                                         HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaint resolved successfully",
                raceComplaintService.refereeResolve(id, body, request));
    }

    @PutMapping("/api/referee/complaints/races/{id}/reject")
    public ApiResponse<?> refereeReject(@PathVariable Integer id,
                                        @RequestBody RaceComplaintRequest body,
                                        HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaint rejected successfully",
                raceComplaintService.refereeReject(id, body, request));
    }

    @PutMapping("/api/referee/complaints/races/{id}/forward")
    public ApiResponse<?> refereeForward(@PathVariable Integer id,
                                         @RequestBody RaceComplaintRequest body,
                                         HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaint forwarded successfully",
                raceComplaintService.refereeForward(id, body, request));
    }

    @GetMapping("/api/organizer/complaints/races")
    public ApiResponse<?> getOrganizerComplaints(HttpServletRequest request) {
        return ApiResponse.success(200, "Forwarded race complaints loaded successfully",
                raceComplaintService.getOrganizerComplaints(request));
    }

    @PutMapping("/api/organizer/complaints/races/{id}/resolve")
    public ApiResponse<?> organizerResolve(@PathVariable Integer id,
                                           @RequestBody(required = false) RaceComplaintRequest body,
                                           HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaint resolved successfully",
                raceComplaintService.organizerResolve(id, body, request));
    }

    @PutMapping("/api/organizer/complaints/races/{id}/reject")
    public ApiResponse<?> organizerReject(@PathVariable Integer id,
                                          @RequestBody RaceComplaintRequest body,
                                          HttpServletRequest request) {
        return ApiResponse.success(200, "Race complaint rejected successfully",
                raceComplaintService.organizerReject(id, body, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
