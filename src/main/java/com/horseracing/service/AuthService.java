package com.horseracing.service;

import com.horseracing.dto.ForgotPasswordRequest;
import com.horseracing.dto.LoginRequest;
import com.horseracing.dto.LoginResponse;
import com.horseracing.dto.RegisterRequest;
import com.horseracing.dto.UserResponse;
import com.horseracing.entity.Role;
import com.horseracing.entity.User;
import com.horseracing.repository.RoleRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        validateRequired(request.getUsername(), "username khong duoc de trong");
        validateRequired(request.getPassword(), "password khong duoc de trong");
        validateRequired(request.getFullName(), "fullName khong duoc de trong");
        validateRequired(request.getEmail(), "email khong duoc de trong");

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username da ton tai");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email da ton tai");
        }

        Role role = resolveRole(request);
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setIsActive(true);
        user.setIsApproved(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return toUserResponse(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        validateRequired(request.getUsername(), "username khong duoc de trong");
        validateRequired(request.getPassword(), "password khong duoc de trong");

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Sai username hoac password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tai khoan dang bi khoa");
        }

        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Tai khoan chua duoc duyet");
        }

        if (!matchesPassword(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Sai username hoac password");
        }

        String accessToken = "test-access-" + UUID.randomUUID();
        String refreshToken = "test-refresh-" + UUID.randomUUID();

        return new LoginResponse(accessToken, refreshToken, toUserResponse(user));
    }

    public UserResponse forgotPassword(ForgotPasswordRequest request) {
        validateRequired(request.getEmail(), "email khong duoc de trong");
        validateRequired(request.getNewPassword(), "newPassword khong duoc de trong");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan voi email nay"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        return toUserResponse(userRepository.save(user));
    }

    private Role resolveRole(RegisterRequest request) {
        if (request.getRoleId() != null) {
            return roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("roleId khong ton tai"));
        }

        String roleName = request.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            roleName = "Spectator";
        }

        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("roleName khong ton tai"));
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return rawPassword.equals(storedPassword);
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
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
