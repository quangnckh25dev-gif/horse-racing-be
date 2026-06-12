package com.horseracing.service;

import com.horseracing.dto.PermissionResponse;
import com.horseracing.dto.RolePermissionRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<PermissionResponse> getPermissions() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT PermissionID, PermissionName, Description
                FROM Permissions
                ORDER BY PermissionID
                """)
                .getResultList();

        return rows.stream().map(this::toPermissionResponse).toList();
    }

    public List<PermissionResponse> getRolePermissions(Integer roleId) {
        ensureRoleExists(roleId);

        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT p.PermissionID, p.PermissionName, p.Description
                FROM RolePermissions rp
                JOIN Permissions p ON rp.PermissionID = p.PermissionID
                WHERE rp.RoleID = :roleId
                ORDER BY p.PermissionID
                """)
                .setParameter("roleId", roleId)
                .getResultList();

        return rows.stream().map(this::toPermissionResponse).toList();
    }

    @Transactional
    public List<PermissionResponse> addPermissionToRole(Integer roleId, RolePermissionRequest request) {
        ensureRoleExists(roleId);
        ensurePermissionExists(request == null ? null : request.getPermissionId());

        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(1)
                        FROM RolePermissions
                        WHERE RoleID = :roleId AND PermissionID = :permissionId
                        """)
                .setParameter("roleId", roleId)
                .setParameter("permissionId", request.getPermissionId())
                .getSingleResult();

        if (count.intValue() == 0) {
            entityManager.createNativeQuery("""
                            INSERT INTO RolePermissions (RoleID, PermissionID)
                            VALUES (:roleId, :permissionId)
                            """)
                    .setParameter("roleId", roleId)
                    .setParameter("permissionId", request.getPermissionId())
                    .executeUpdate();
        }

        return getRolePermissions(roleId);
    }

    @Transactional
    public void removePermissionFromRole(Integer roleId, Integer permissionId) {
        ensureRoleExists(roleId);
        ensurePermissionExists(permissionId);

        entityManager.createNativeQuery("""
                        DELETE FROM RolePermissions
                        WHERE RoleID = :roleId AND PermissionID = :permissionId
                        """)
                .setParameter("roleId", roleId)
                .setParameter("permissionId", permissionId)
                .executeUpdate();
    }

    private void ensureRoleExists(Integer roleId) {
        if (roleId == null || countById("Roles", "RoleID", roleId) == 0) {
            throw new IllegalArgumentException("Khong tim thay role");
        }
    }

    private void ensurePermissionExists(Integer permissionId) {
        if (permissionId == null || countById("Permissions", "PermissionID", permissionId) == 0) {
            throw new IllegalArgumentException("Khong tim thay permission");
        }
    }

    private int countById(String tableName, String columnName, Integer id) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(1) FROM " + tableName + " WHERE " + columnName + " = :id")
                .setParameter("id", id)
                .getSingleResult();
        return count.intValue();
    }

    private PermissionResponse toPermissionResponse(Object[] row) {
        return new PermissionResponse(
                ((Number) row[0]).intValue(),
                row[1] == null ? null : row[1].toString(),
                row[2] == null ? null : row[2].toString()
        );
    }
}
