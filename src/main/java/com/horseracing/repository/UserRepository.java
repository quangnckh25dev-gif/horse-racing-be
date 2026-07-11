package com.horseracing.repository;

import com.horseracing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByResetToken(String resetToken);

    java.util.List<User> findByIsApprovedFalse();

    java.util.List<User> findByIsActiveTrue();

    java.util.List<User> findByRole_RoleNameAndIsActiveTrue(String roleName);

    @Query("""
            select u from User u
            where u.isActive = true
              and u.isApproved = true
              and u.role.roleName in ('Admin', 'Organizer')
            """)
    java.util.List<User> findActiveOrganizersAndAdmins();
}
