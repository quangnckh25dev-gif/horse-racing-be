package com.horseracing.controller;

import com.horseracing.dto.SystemConfigUpdateRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/configs")
@CrossOrigin("*")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ApiResponse<?> getConfigs(HttpServletRequest request) {
        return ApiResponse.success(
                200,
                "Lay cau hinh he thong thanh cong",
                systemConfigService.getConfigs(request)
        );
    }

    @PutMapping("/{key}")
    public ApiResponse<?> updateConfig(
            @PathVariable String key,
            @RequestBody SystemConfigUpdateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                200,
                "Cap nhat cau hinh thanh cong",
                systemConfigService.updateConfig(key, body, request)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleForbidden(SecurityException ex) {
        return ApiResponse.error(403, ex.getMessage());
    }
}
