package com.horseracing.service;

import com.horseracing.dto.ForgotPasswordRequest;
import com.horseracing.dto.LoginRequest;
import com.horseracing.dto.LoginResponse;
import com.horseracing.dto.RegisterRequest;
import com.horseracing.dto.ResetPasswordRequest;
import com.horseracing.dto.TokenRequest;
import com.horseracing.dto.ChangePasswordRequest;
import com.horseracing.dto.UserTokenResponse;
import com.horseracing.dto.UserResponse;
import com.horseracing.entity.Role;
import com.horseracing.entity.User;
import com.horseracing.entity.UserToken;
import com.horseracing.repository.RoleRepository;
import com.horseracing.repository.UserTokenRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserTokenRepository userTokenRepository;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, EmailService emailService, UserTokenRepository userTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.userTokenRepository = userTokenRepository;
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

        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("So dien thoai da ton tai");
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
        user.setIsApproved(false); // Changed to false for Admin approval
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return toUserResponse(userRepository.save(user));
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResponse login(LoginRequest request) {
        validateRequired(request.getUsername(), "username khong duoc de trong");
        validateRequired(request.getPassword(), "password khong duoc de trong");

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Sai username hoac password"));

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new IllegalArgumentException("Tai khoan da bi khoa do dang nhap sai qua nhieu lan");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tai khoan dang bi khoa");
        }

        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Tai khoan chua duoc duyet");
        }

        if (!matchesPassword(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
            failedAttempts++;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setIsLocked(true);
                user.setIsActive(false);
                userRepository.save(user);
                throw new IllegalArgumentException("Tai khoan da bi khoa do dang nhap sai qua nhieu lan");
            }
            userRepository.save(user);
            int remaining = 5 - failedAttempts;
            throw new IllegalArgumentException("Sai mat khau. Con " + remaining + " lan thu.");
        }

        // Login successful
        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Spectator";
        String accessToken = jwtService.generateToken(user.getUsername(), roleName);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        saveRefreshToken(user.getUserId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken, toUserResponse(user));
    }

    @Transactional
    public LoginResponse refreshToken(TokenRequest request) {
        validateRequired(request.getRefreshToken(), "refreshToken khong duoc de trong");

        UserToken storedToken = userTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token khong hop le"));

        if (Boolean.TRUE.equals(storedToken.getIsRevoked()) || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token da het han hoac da bi thu hoi");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user cua token"));

        if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Tai khoan khong duoc phep refresh token");
        }

        storedToken.setIsRevoked(true);
        userTokenRepository.save(storedToken);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Spectator";
        String accessToken = jwtService.generateToken(user.getUsername(), roleName);
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());
        saveRefreshToken(user.getUserId(), newRefreshToken);

        return new LoginResponse(accessToken, newRefreshToken, toUserResponse(user));
    }

    @Transactional
    public void logout(TokenRequest request) {
        validateRequired(request.getRefreshToken(), "refreshToken khong duoc de trong");

        UserToken storedToken = userTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token khong hop le"));

        storedToken.setIsRevoked(true);
        userTokenRepository.save(storedToken);
    }

    public java.util.List<UserTokenResponse> getUserTokens(Integer userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Khong tim thay user");
        }

        return userTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toUserTokenResponse)
                .toList();
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        validateRequired(request.getEmail(), "email khong duoc de trong");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan voi email nay"));

        String resetToken = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Gửi email thực tế
        emailService.sendResetTokenEmail(user.getEmail(), resetToken);
        
        return "Mã xác nhận đã được gửi đến email của bạn";
    }

    public UserResponse resetPasswordWithToken(ResetPasswordRequest request) {
        validateRequired(request.getToken(), "token khong duoc de trong");
        validateRequired(request.getNewPassword(), "newPassword khong duoc de trong");

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token khong hop le hoac khong tim thay"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token da het han");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());

        return toUserResponse(userRepository.save(user));
    }

    public UserResponse changePassword(String username, ChangePasswordRequest request) {
        validateRequired(request.getOldPassword(), "oldPassword khong duoc de trong");
        validateRequired(request.getNewPassword(), "newPassword khong duoc de trong");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan"));

        if (!matchesPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau cu khong chinh xac");
        }

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

    private void saveRefreshToken(Integer userId, String refreshToken) {
        UserToken userToken = new UserToken();
        userToken.setUserId(userId);
        userToken.setToken(refreshToken);
        userToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        userToken.setIsRevoked(false);
        userTokenRepository.save(userToken);
    }

    private UserTokenResponse toUserTokenResponse(UserToken token) {
        return new UserTokenResponse(
                token.getTokenId(),
                token.getUserId(),
                token.getToken(),
                token.getExpiresAt(),
                token.getIsRevoked(),
                token.getCreatedAt()
        );
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
