package com.horseracing.controller;

import com.horseracing.dto.DepositComplaintRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.DepositComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class DepositComplaintController {

    private final DepositComplaintService depositComplaintService;

    public DepositComplaintController(DepositComplaintService depositComplaintService) {
        this.depositComplaintService = depositComplaintService;
    }

    @PostMapping("/api/complaints/deposits")
    public ApiResponse<?> createComplaint(@RequestBody DepositComplaintRequest body,
                                          HttpServletRequest request) {
        return ApiResponse.success(201, "Deposit complaint created successfully",
                depositComplaintService.createComplaint(body, request));
    }

    @GetMapping("/api/complaints/deposits/mine")
    public ApiResponse<?> getMyComplaints(HttpServletRequest request) {
        return ApiResponse.success(200, "My deposit complaints loaded successfully",
                depositComplaintService.getMyComplaints(request));
    }

    @GetMapping("/api/admin/complaints/deposits")
    public ApiResponse<?> getAllComplaints(HttpServletRequest request) {
        return ApiResponse.success(200, "Deposit complaints loaded successfully",
                depositComplaintService.getAllComplaints(request));
    }

    @PutMapping("/api/admin/complaints/deposits/{id}/resolve")
    public ApiResponse<?> resolveComplaint(@PathVariable Integer id,
                                           @RequestBody(required = false) DepositComplaintRequest body,
                                           HttpServletRequest request) {
        return ApiResponse.success(200, "Deposit complaint resolved successfully",
                depositComplaintService.resolveComplaint(id, body, request));
    }

    @PutMapping("/api/admin/complaints/deposits/{id}/reject")
    public ApiResponse<?> rejectComplaint(@PathVariable Integer id,
                                          @RequestBody DepositComplaintRequest body,
                                          HttpServletRequest request) {
        return ApiResponse.success(200, "Deposit complaint rejected successfully",
                depositComplaintService.rejectComplaint(id, body, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
