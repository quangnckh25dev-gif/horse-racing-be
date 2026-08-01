package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import com.horseracing.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@CrossOrigin("*")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<?> getLogs(HttpServletRequest request,
                                  @RequestParam(required = false) String action,
                                  @RequestParam(required = false) String tableName,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String date) {
        return ApiResponse.success(
                200,
                "Audit logs loaded successfully",
                auditLogService.getLogs(request, action, tableName, keyword, date)
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
