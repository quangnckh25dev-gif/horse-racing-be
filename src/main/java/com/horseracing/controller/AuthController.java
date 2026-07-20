package com.horseracing.controller;

import com.horseracing.dto.ForgotPasswordRequest;
import com.horseracing.dto.LoginRequest;
import com.horseracing.dto.RegisterRequest;
import com.horseracing.dto.TokenRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.horseracing.dto.ChangePasswordRequest;
import com.horseracing.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {
    private final AuthService authService;
    private final com.horseracing.service.JwtService jwtService;

    public AuthController(AuthService authService, com.horseracing.service.JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(201, "Account created successfully", authService.register(request));
    }

    @GetMapping("/register-roles")
    public ApiResponse<?> getRegisterRoles() {
        return ApiResponse.success(200, "Registration roles loaded successfully", authService.getRegisterRoleOptions());
    }

    @PostMapping("/create-account")
    public ApiResponse<?> createAccount(@RequestBody RegisterRequest request) {
        return ApiResponse.success(201, "Account created successfully", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(200, "Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refresh(@RequestBody TokenRequest request) {
        return ApiResponse.success(200, "Token refreshed successfully", authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestBody TokenRequest request) {
        authService.logout(request);
        return ApiResponse.success(200, "Logout successful", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request);
        return ApiResponse.success(200, message, null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ApiResponse.success(200, "Password reset successfully", authService.resetPasswordWithToken(request));
    }

    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(jakarta.servlet.http.HttpServletRequest httpRequest, @RequestBody ChangePasswordRequest request) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error(401, "Missing or invalid token");
        }
        
        try {
            String token = authHeader.substring(7);
            String username = jwtService.extractClaims(token).getSubject();
            return ApiResponse.success(200, "Password changed successfully", authService.changePassword(username, request));
        } catch (Exception e) {
            return ApiResponse.error(401, "Invalid or expired token");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
