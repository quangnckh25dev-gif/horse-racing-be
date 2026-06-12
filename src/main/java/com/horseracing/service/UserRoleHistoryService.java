package com.horseracing.service;

import com.horseracing.dto.UserRoleHistoryResponse;
import com.horseracing.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserRoleHistoryService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public UserRoleHistoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserRoleHistoryResponse> getAllRoleHistory() {
        List<Object[]> rows = entityManager.createNativeQuery(baseQuery() + " ORDER BY h.ChangedAt DESC")
                .getResultList();
        return rows.stream().map(this::toResponse).toList();
    }

    public List<UserRoleHistoryResponse> getRoleHistoryByUser(Integer userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Khong tim thay user");
        }

        List<Object[]> rows = entityManager.createNativeQuery(baseQuery() + """
                        WHERE h.UserID = :userId
                        ORDER BY h.ChangedAt DESC
                        """)
                .setParameter("userId", userId)
                .getResultList();

        return rows.stream().map(this::toResponse).toList();
    }

    private String baseQuery() {
        return """
                SELECT
                    h.HistoryID,
                    h.UserID,
                    u.Username,
                    h.OldRoleID,
                    oldRole.RoleName AS OldRoleName,
                    h.NewRoleID,
                    newRole.RoleName AS NewRoleName,
                    h.ChangedBy,
                    adminUser.Username AS ChangedByUsername,
                    h.ChangedAt
                FROM UserRoleHistory h
                JOIN Users u ON h.UserID = u.UserID
                LEFT JOIN Roles oldRole ON h.OldRoleID = oldRole.RoleID
                JOIN Roles newRole ON h.NewRoleID = newRole.RoleID
                LEFT JOIN Users adminUser ON h.ChangedBy = adminUser.UserID
                """;
    }

    private UserRoleHistoryResponse toResponse(Object[] row) {
        return new UserRoleHistoryResponse(
                toInteger(row[0]),
                toInteger(row[1]),
                toString(row[2]),
                toInteger(row[3]),
                toString(row[4]),
                toInteger(row[5]),
                toString(row[6]),
                toInteger(row[7]),
                toString(row[8]),
                toLocalDateTime(row[9])
        );
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
