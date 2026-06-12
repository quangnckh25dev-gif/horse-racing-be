package com.horseracing.service;

import com.horseracing.dto.AuditLogResponse;
import com.horseracing.entity.AuditLog;
import com.horseracing.entity.User;
import com.horseracing.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditLogService(AuditLogRepository auditLogRepository, CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public List<AuditLogResponse> getLogs(HttpServletRequest request) {
        requireAdmin(request);
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void log(Integer userId, String action, String tableName, Integer recordId,
                    String oldValue, String newValue, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setTableName(tableName);
        auditLog.setRecordId(recordId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }

    public String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void requireAdmin(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        if (!currentUserService.isAdmin(user)) {
            throw new SecurityException("Chi admin moi co quyen xem audit log");
        }
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getLogId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getTableName(),
                auditLog.getRecordId(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt()
        );
    }
}
