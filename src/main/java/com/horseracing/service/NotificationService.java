package com.horseracing.service;

import com.horseracing.dto.NotificationRequest;
import com.horseracing.dto.NotificationResponse;
import com.horseracing.entity.Notification;
import com.horseracing.repository.NotificationRepository;
import com.horseracing.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationResponse> getUserNotifications(Integer userId) {
        ensureUserExists(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public int countUnreadNotifications(Integer userId) {
        ensureUserExists(userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        validateRequest(request);
        ensureUserExists(request.getUserId());

        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle().trim());
        notification.setBody(request.getBody());
        notification.setNotifType(request.getNotifType());
        notification.setRelatedEntityId(request.getRelatedEntityId());
        notification.setRelatedEntity(request.getRelatedEntity());
        notification.setIsRead(false);

        return toResponse(notificationRepository.save(notification));
    }

    public NotificationResponse markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay notification"));

        notification.setIsRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllAsRead(Integer userId) {
        ensureUserExists(userId);
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    private void validateRequest(NotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu notification khong hop le");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId khong duoc de trong");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title khong duoc de trong");
        }
    }

    private void ensureUserExists(Integer userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Khong tim thay user");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getNotifType(),
                notification.getRelatedEntityId(),
                notification.getRelatedEntity(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
