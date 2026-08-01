package com.horseracing.controller;

import com.horseracing.dto.RoleChangeRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin("*")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/pending")
    public ApiResponse<?> getPendingUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                200,
                "Pending users loaded successfully",
                adminUserService.getPendingUsers(role, keyword)
        );
    }

    @GetMapping
    public ApiResponse<?> getAllActiveUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                200,
                "Users loaded successfully",
                adminUserService.getAllActiveUsers(role, status, keyword)
        );
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<?> approveUser(@PathVariable Integer id, @RequestParam(defaultValue = "1") Integer adminId) {
        return ApiResponse.success(200, "User approved successfully", adminUserService.approveUser(id, adminId));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<?> rejectUser(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer adminId,
            @RequestParam(required = false) String reason
    ) {
        return ApiResponse.success(200, "User rejected successfully", adminUserService.rejectUser(id, adminId, reason));
    }

    @PutMapping("/{id}/role")
    public ApiResponse<?> changeUserRole(
            @PathVariable Integer id,
            @RequestBody RoleChangeRequest request,
            @RequestParam(defaultValue = "1") Integer adminId
    ) {
        return ApiResponse.success(
                200,
                "User role changed successfully",
                adminUserService.changeUserRole(id, request.getRoleName(), adminId)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
