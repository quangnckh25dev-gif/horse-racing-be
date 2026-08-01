package com.horseracing.controller;

import com.horseracing.dto.RaceEntryApproveRequest;
import com.horseracing.dto.RaceEntryRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.RaceEntryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class RaceEntryController {

    private final RaceEntryService raceEntryService;

    public RaceEntryController(RaceEntryService raceEntryService) {
        this.raceEntryService = raceEntryService;
    }

    @GetMapping("/api/races/{raceId}/entries")
    public ApiResponse<?> getRaceEntries(@PathVariable Integer raceId) {
        return ApiResponse.success(200, "Race entries loaded successfully", raceEntryService.getRaceEntries(raceId));
    }

    @GetMapping("/api/races/{raceId}/spectator-entries")
    public ApiResponse<?> getSpectatorRaceEntries(@PathVariable Integer raceId) {
        return ApiResponse.success(200, "Spectator race entries loaded successfully", raceEntryService.getPublicRaceEntries(raceId));
    }

    @GetMapping("/api/entries/mine")
    public ApiResponse<?> getMyEntries(HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Owner entries loaded successfully", raceEntryService.getMyEntries(httpRequest));
    }

    @GetMapping("/api/entries/mine/approved")
    public ApiResponse<?> getMyApprovedEntries(HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Approved entries loaded successfully", raceEntryService.getMyApprovedEntries(httpRequest));
    }

    @PostMapping("/api/races/{raceId}/entries")
    public ApiResponse<?> registerHorse(@PathVariable Integer raceId, @RequestBody RaceEntryRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(201, "Horse registered for race successfully", raceEntryService.registerHorse(raceId, request, httpRequest));
    }

    @DeleteMapping("/api/races/{raceId}/entries/{entryId}")
    public ApiResponse<?> withdrawEntry(@PathVariable Integer raceId, @PathVariable Integer entryId, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Entry withdrawn successfully", raceEntryService.withdrawEntry(raceId, entryId, httpRequest));
    }

    @PatchMapping("/api/races/{raceId}/entries/{entryId}/approve")
    public ApiResponse<?> approveEntry(@PathVariable Integer raceId, @PathVariable Integer entryId,
                                       @RequestBody RaceEntryApproveRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Entry approval updated successfully", raceEntryService.approveEntry(raceId, entryId, request, httpRequest));
    }

    @GetMapping("/api/entries/{entryId}")
    public ApiResponse<?> getEntry(@PathVariable Integer entryId, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Entry detail loaded successfully", raceEntryService.getEntry(entryId, httpRequest));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
