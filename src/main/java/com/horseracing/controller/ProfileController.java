package com.horseracing.controller;

import com.horseracing.dto.ProfileRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/api/profile/owner/{userId}")
    public ApiResponse<?> getOwnerProfile(@PathVariable Integer userId) {
        return ApiResponse.success(200, "Lay owner profile thanh cong", profileService.getOwnerProfile(userId));
    }

    @PutMapping("/api/profile/owner/{userId}")
    public ApiResponse<?> updateOwnerProfile(@PathVariable Integer userId, @RequestBody ProfileRequest request) {
        return ApiResponse.success(200, "Cap nhat owner profile thanh cong", profileService.updateOwnerProfile(userId, request));
    }

    @GetMapping("/api/profile/jockey/{userId}")
    public ApiResponse<?> getJockeyProfile(@PathVariable Integer userId) {
        return ApiResponse.success(200, "Lay jockey profile thanh cong", profileService.getJockeyProfile(userId));
    }

    @PutMapping("/api/profile/jockey/{userId}")
    public ApiResponse<?> updateJockeyProfile(@PathVariable Integer userId, @RequestBody ProfileRequest request) {
        return ApiResponse.success(200, "Cap nhat jockey profile thanh cong", profileService.updateJockeyProfile(userId, request));
    }

    @GetMapping("/api/profile/referee/{userId}")
    public ApiResponse<?> getRefereeProfile(@PathVariable Integer userId) {
        return ApiResponse.success(200, "Lay referee profile thanh cong", profileService.getRefereeProfile(userId));
    }

    @PutMapping("/api/profile/referee/{userId}")
    public ApiResponse<?> updateRefereeProfile(@PathVariable Integer userId, @RequestBody ProfileRequest request) {
        return ApiResponse.success(200, "Cap nhat referee profile thanh cong", profileService.updateRefereeProfile(userId, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
