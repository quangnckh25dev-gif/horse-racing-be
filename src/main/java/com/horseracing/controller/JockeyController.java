package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.JockeyLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class JockeyController {

    private final JockeyLookupService jockeyLookupService;

    public JockeyController(JockeyLookupService jockeyLookupService) {
        this.jockeyLookupService = jockeyLookupService;
    }

    @GetMapping("/api/jockeys")
    public ApiResponse<?> getJockeys() {
        return ApiResponse.success(200, "Lay danh sach jockey thanh cong", jockeyLookupService.getActiveJockeys());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}