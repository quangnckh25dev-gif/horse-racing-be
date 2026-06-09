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
    public ApiResponse<?> getPendingUsers() {
        return ApiResponse.success(200, "Lay danh sach cho duyet thanh cong", adminUserService.getPendingUsers());
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<?> approveUser(@PathVariable Integer id, @RequestParam(defaultValue = "1") Integer adminId) {
        return ApiResponse.success(200, "Duyet user thanh cong", adminUserService.approveUser(id, adminId));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<?> rejectUser(@PathVariable Integer id, @RequestParam(defaultValue = "1") Integer adminId) {
        return ApiResponse.success(200, "Tu choi user thanh cong", adminUserService.rejectUser(id, adminId));
    }

    @PutMapping("/{id}/role")
    public ApiResponse<?> changeUserRole(@PathVariable Integer id, @RequestBody RoleChangeRequest request, @RequestParam(defaultValue = "1") Integer adminId) {
        return ApiResponse.success(200, "Doi role thanh cong", adminUserService.changeUserRole(id, request.getRoleName(), adminId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
