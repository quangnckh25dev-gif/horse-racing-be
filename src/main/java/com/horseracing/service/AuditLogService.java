package com.horseracing.service;

import com.horseracing.dto.AuditLogResponse;
import com.horseracing.entity.AuditLog;
import com.horseracing.entity.User;
import com.horseracing.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditLogService(AuditLogRepository auditLogRepository, CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public List<AuditLogResponse> getLogs(HttpServletRequest request, String action, String tableName,
                                          String keyword, String date) {
        requireAdmin(request);
        String actionFilter = trimToNull(action);
        String tableFilter = trimToNull(tableName);
        String keywordFilter = trimToNull(keyword);
        LocalDate dateFilter = parseDate(date);
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(log -> matchesText(log.getAction(), actionFilter))
                .filter(log -> matchesText(log.getTableName(), tableFilter))
                .filter(log -> matchesDate(log, dateFilter))
                .filter(log -> matchesKeyword(log, keywordFilter))
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
            throw new SecurityException("Only admins can view audit logs.");
        }
    }

    private boolean matchesText(String value, String filter) {
        return filter == null || (value != null && value.equalsIgnoreCase(filter));
    }

    private boolean matchesDate(AuditLog auditLog, LocalDate date) {
        return date == null || (auditLog.getCreatedAt() != null && date.equals(auditLog.getCreatedAt().toLocalDate()));
    }

    private boolean matchesKeyword(AuditLog auditLog, String keyword) {
        if (keyword == null) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(auditLog.getAction(), normalized)
                || contains(auditLog.getTableName(), normalized)
                || contains(auditLog.getOldValue(), normalized)
                || contains(auditLog.getNewValue(), normalized)
                || contains(auditLog.getIpAddress(), normalized)
                || contains(String.valueOf(auditLog.getUserId()), normalized)
                || contains(String.valueOf(auditLog.getRecordId()), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private LocalDate parseDate(String date) {
        String value = trimToNull(date);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("date must be in yyyy-MM-dd format.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
