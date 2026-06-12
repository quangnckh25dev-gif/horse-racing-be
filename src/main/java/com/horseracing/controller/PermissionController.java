package com.horseracing.controller;

import com.horseracing.dto.RolePermissionRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/api/admin/permissions")
    public ApiResponse<?> getPermissions() {
        return ApiResponse.success(200, "Lay danh sach permission thanh cong", permissionService.getPermissions());
    }

    @GetMapping("/api/admin/roles/{roleId}/permissions")
    public ApiResponse<?> getRolePermissions(@PathVariable Integer roleId) {
        return ApiResponse.success(200, "Lay permission theo role thanh cong", permissionService.getRolePermissions(roleId));
    }

    @PostMapping("/api/admin/roles/{roleId}/permissions")
    public ApiResponse<?> addPermissionToRole(@PathVariable Integer roleId, @RequestBody RolePermissionRequest request) {
        return ApiResponse.success(200, "Gan permission cho role thanh cong", permissionService.addPermissionToRole(roleId, request));
    }

    @DeleteMapping("/api/admin/roles/{roleId}/permissions/{permissionId}")
    public ApiResponse<?> removePermissionFromRole(@PathVariable Integer roleId, @PathVariable Integer permissionId) {
        permissionService.removePermissionFromRole(roleId, permissionId);
        return ApiResponse.success(200, "Go permission khoi role thanh cong", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
