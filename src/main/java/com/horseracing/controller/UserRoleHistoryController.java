package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.UserRoleHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class UserRoleHistoryController {

    private final UserRoleHistoryService userRoleHistoryService;

    public UserRoleHistoryController(UserRoleHistoryService userRoleHistoryService) {
        this.userRoleHistoryService = userRoleHistoryService;
    }

    @GetMapping("/api/admin/user-role-history")
    public ApiResponse<?> getAllRoleHistory() {
        return ApiResponse.success(200, "Lay lich su doi role thanh cong", userRoleHistoryService.getAllRoleHistory());
    }

    @GetMapping("/api/admin/users/{userId}/role-history")
    public ApiResponse<?> getRoleHistoryByUser(@PathVariable Integer userId) {
        return ApiResponse.success(200, "Lay lich su doi role cua user thanh cong", userRoleHistoryService.getRoleHistoryByUser(userId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
