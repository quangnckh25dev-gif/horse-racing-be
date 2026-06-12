package com.horseracing.controller;

import com.horseracing.dto.NotificationRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.NotificationService;
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

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/users/{userId}/notifications")
    public ApiResponse<?> getUserNotifications(@PathVariable Integer userId) {
        return ApiResponse.success(
                200,
                "Lay danh sach notification thanh cong",
                notificationService.getUserNotifications(userId)
        );
    }

    @GetMapping("/api/users/{userId}/notifications/unread-count")
    public ApiResponse<?> countUnreadNotifications(@PathVariable Integer userId) {
        return ApiResponse.success(
                200,
                "Lay so notification chua doc thanh cong",
                notificationService.countUnreadNotifications(userId)
        );
    }

    @PostMapping("/api/notifications")
    public ApiResponse<?> createNotification(@RequestBody NotificationRequest request) {
        return ApiResponse.success(
                201,
                "Tao notification thanh cong",
                notificationService.createNotification(request)
        );
    }

    @PutMapping("/api/notifications/{notificationId}/read")
    public ApiResponse<?> markAsRead(@PathVariable Integer notificationId) {
        return ApiResponse.success(
                200,
                "Danh dau notification da doc thanh cong",
                notificationService.markAsRead(notificationId)
        );
    }

    @PutMapping("/api/users/{userId}/notifications/read-all")
    public ApiResponse<?> markAllAsRead(@PathVariable Integer userId) {
        return ApiResponse.success(
                200,
                "Danh dau tat ca notification da doc thanh cong",
                notificationService.markAllAsRead(userId)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
