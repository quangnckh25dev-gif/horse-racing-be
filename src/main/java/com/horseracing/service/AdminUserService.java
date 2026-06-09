package com.horseracing.service;

import com.horseracing.dto.UserResponse;
import com.horseracing.entity.AuditLog;
import com.horseracing.entity.Role;
import com.horseracing.entity.User;
import com.horseracing.entity.UserRoleHistory;
import com.horseracing.repository.AuditLogRepository;
import com.horseracing.repository.RoleRepository;
import com.horseracing.repository.UserRepository;
import com.horseracing.repository.UserRoleHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository, AuditLogRepository auditLogRepository, UserRoleHistoryRepository userRoleHistoryRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRoleHistoryRepository = userRoleHistoryRepository;
    }

    public List<UserResponse> getPendingUsers() {
        return userRepository.findByIsApprovedFalse().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse approveUser(Integer targetUserId, Integer adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user"));
        
        user.setIsApproved(true);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        logAudit("APPROVE_USER", adminId, "Users", targetUserId, "Pending", "Approved");

        return toUserResponse(savedUser);
    }

    public UserResponse rejectUser(Integer targetUserId, Integer adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user"));
        
        user.setIsApproved(false);
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        logAudit("REJECT_USER", adminId, "Users", targetUserId, "Pending", "Rejected");

        return toUserResponse(savedUser);
    }

    public UserResponse changeUserRole(Integer targetUserId, String roleName, Integer adminId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user"));

        Role newRole = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role khong ton tai"));

        Integer oldRoleId = user.getRole() != null ? user.getRole().getRoleId() : null;

        if (oldRoleId != null && oldRoleId.equals(newRole.getRoleId())) {
            throw new IllegalArgumentException("User da co role nay roi");
        }

        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        UserRoleHistory history = new UserRoleHistory();
        history.setUserId(targetUserId);
        history.setOldRoleId(oldRoleId);
        history.setNewRoleId(newRole.getRoleId());
        history.setChangedBy(adminId);
        history.setChangedAt(LocalDateTime.now());
        userRoleHistoryRepository.save(history);

        logAudit("CHANGE_ROLE", adminId, "Users", targetUserId, String.valueOf(oldRoleId), String.valueOf(newRole.getRoleId()));

        return toUserResponse(savedUser);
    }

    private void logAudit(String action, Integer performedBy, String tableName, Integer recordId, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUserId(performedBy);
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private UserResponse toUserResponse(User user) {
        String roleName = user.getRole() == null ? null : user.getRole().getRoleName();
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                roleName,
                user.getIsActive(),
                user.getIsApproved()
        );
    }
}
