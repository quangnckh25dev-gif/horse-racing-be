package com.horseracing.repository;

import com.horseracing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByResetToken(String resetToken);

    java.util.List<User> findByIsApprovedFalse();

    @Query("""
            select u from User u
            where u.isApproved = false
              and u.isActive = true
              and u.role is not null
              and lower(u.role.roleName) not in ('admin', 'spectator')
              and (:role is null or lower(u.role.roleName) = lower(:role))
              and (
                    :keyword is null
                    or lower(coalesce(u.username, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.fullName, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
              )
            order by u.createdAt desc
            """)
    java.util.List<User> findPendingUsers(@Param("role") String role, @Param("keyword") String keyword);

    java.util.List<User> findByIsActiveTrue();

    @Query("""
            select u from User u
            where u.role is not null
              and lower(u.role.roleName) <> 'admin'
              and (:role is null or lower(u.role.roleName) = lower(:role))
              and (:active is null or u.isActive = :active)
              and (
                    :keyword is null
                    or lower(coalesce(u.username, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.fullName, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
              )
            order by u.createdAt desc
            """)
    java.util.List<User> findManagedUsers(
            @Param("role") String role,
            @Param("active") Boolean active,
            @Param("keyword") String keyword
    );

    java.util.List<User> findByRole_RoleNameAndIsActiveTrue(String roleName);

    @Query("""
            select u from User u
            where u.isActive = true
              and u.isApproved = true
              and u.role.roleName in ('Admin', 'Organizer')
            """)
    java.util.List<User> findActiveOrganizersAndAdmins();
}
