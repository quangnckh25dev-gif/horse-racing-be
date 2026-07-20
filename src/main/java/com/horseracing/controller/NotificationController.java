package com.horseracing.controller;

import com.horseracing.dto.NotificationRequest;
import com.horseracing.entity.User;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.CurrentUserService;
import com.horseracing.service.NotificationService;
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
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/notifications/me")
    public ApiResponse<?> getMyNotifications(HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(
                200,
                "Notifications loaded successfully",
                notificationService.getUserNotifications(currentUser.getUserId())
        );
    }

    @GetMapping("/api/notifications/me/unread-count")
    public ApiResponse<?> countMyUnreadNotifications(HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(
                200,
                "Unread notification count loaded successfully",
                notificationService.countUnreadNotifications(currentUser.getUserId())
        );
    }

    @PostMapping("/api/notifications")
    public ApiResponse<?> createNotification(@RequestBody NotificationRequest request) {
        return ApiResponse.success(
                201,
                "Notification created successfully",
                notificationService.createNotification(request)
        );
    }

    @PutMapping("/api/notifications/{notificationId}/read")
    public ApiResponse<?> markAsRead(@PathVariable Integer notificationId, HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(
                200,
                "Notification marked as read successfully",
                notificationService.markAsRead(notificationId, currentUser.getUserId())
        );
    }

    @PutMapping("/api/notifications/me/read-all")
    public ApiResponse<?> markMyAllAsRead(HttpServletRequest request) {
        User currentUser = currentUserService.getCurrentUser(request);
        return ApiResponse.success(
                200,
                "All notifications marked as read successfully",
                notificationService.markAllAsRead(currentUser.getUserId())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
