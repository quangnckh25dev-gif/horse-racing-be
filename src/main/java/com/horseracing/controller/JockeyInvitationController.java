package com.horseracing.controller;

import com.horseracing.dto.JockeyInvitationRequest;
import com.horseracing.dto.JockeyInvitationRespondRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.JockeyInvitationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class JockeyInvitationController {

    private final JockeyInvitationService invitationService;

    public JockeyInvitationController(JockeyInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/api/entries/{entryId}/invitations")
    public ApiResponse<?> sendInvitation(@PathVariable Integer entryId, @RequestBody JockeyInvitationRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(201, "Gui loi moi jockey thanh cong", invitationService.sendInvitation(entryId, request, httpRequest));
    }

    @GetMapping("/api/invitations/received")
    public ApiResponse<?> getReceivedInvitations(HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Lay danh sach invitation da nhan thanh cong", invitationService.getReceivedInvitations(httpRequest));
    }

    @PatchMapping("/api/invitations/{invitationId}/respond")
    public ApiResponse<?> respondInvitation(@PathVariable Integer invitationId, @RequestBody JockeyInvitationRespondRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Phan hoi invitation thanh cong", invitationService.respondInvitation(invitationId, request, httpRequest));
    }

    @GetMapping("/api/invitations/sent")
    public ApiResponse<?> getSentInvitations(HttpServletRequest httpRequest) {
        return ApiResponse.success(200, "Lay danh sach invitation da gui thanh cong", invitationService.getSentInvitations(httpRequest));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}