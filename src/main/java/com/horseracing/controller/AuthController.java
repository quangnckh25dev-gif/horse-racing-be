package com.horseracing.controller;

import com.horseracing.dto.ForgotPasswordRequest;
import com.horseracing.dto.LoginRequest;
import com.horseracing.dto.RegisterRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(201, "Tao tai khoan thanh cong", authService.register(request));
    }

    @PostMapping("/create-account")
    public ApiResponse<?> createAccount(@RequestBody RegisterRequest request) {
        return ApiResponse.success(201, "Tao tai khoan thanh cong", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(200, "Dang nhap thanh cong", authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success(200, "Doi mat khau thanh cong", authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success(200, "Doi mat khau thanh cong", authService.forgotPassword(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
