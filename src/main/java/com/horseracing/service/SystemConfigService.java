package com.horseracing.service;

import com.horseracing.dto.SystemConfigResponse;
import com.horseracing.dto.SystemConfigUpdateRequest;
import com.horseracing.entity.SystemConfig;
import com.horseracing.entity.User;
import com.horseracing.repository.SystemConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public SystemConfigService(
            SystemConfigRepository systemConfigRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.systemConfigRepository = systemConfigRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public List<SystemConfigResponse> getConfigs(HttpServletRequest request) {
        requireAdmin(request);
        return systemConfigRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SystemConfigResponse updateConfig(String key, SystemConfigUpdateRequest request, HttpServletRequest httpRequest) {
        User admin = requireAdmin(httpRequest);
        if (request == null || request.getValue() == null) {
            throw new IllegalArgumentException("value khong duoc de trong");
        }

        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay config"));

        String oldValue = config.getConfigValue();
        config.setConfigValue(request.getValue());
        config.setUpdatedBy(admin.getUserId());
        SystemConfig saved = systemConfigRepository.save(config);

        auditLogService.log(
                admin.getUserId(),
                "UPDATE_CONFIG",
                "SystemConfigs",
                saved.getConfigId(),
                oldValue,
                saved.getConfigValue(),
                auditLogService.getClientIp(httpRequest)
        );

        return toResponse(saved);
    }

    private User requireAdmin(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        if (!currentUserService.isAdmin(user)) {
            throw new SecurityException("Chi admin moi co quyen cau hinh he thong");
        }
        return user;
    }

    private SystemConfigResponse toResponse(SystemConfig config) {
        return new SystemConfigResponse(
                config.getConfigId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription(),
                config.getUpdatedBy(),
                config.getUpdatedAt()
        );
    }
}
