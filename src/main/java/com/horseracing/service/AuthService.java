package com.horseracing.service;

import com.horseracing.dto.ChangePasswordRequest;
import com.horseracing.dto.ForgotPasswordRequest;
import com.horseracing.dto.LoginRequest;
import com.horseracing.dto.LoginResponse;
import com.horseracing.dto.OptionResponse;
import com.horseracing.dto.RegisterRequest;
import com.horseracing.dto.ResetPasswordRequest;
import com.horseracing.dto.TokenRequest;
import com.horseracing.dto.UserResponse;
import com.horseracing.entity.Role;
import com.horseracing.entity.User;
import com.horseracing.entity.UserToken;
import com.horseracing.repository.RoleRepository;
import com.horseracing.repository.SystemConfigRepository;
import com.horseracing.repository.UserRepository;
import com.horseracing.repository.UserTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserTokenRepository userTokenRepository;
    private final SystemConfigRepository systemConfigRepository;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, EmailService emailService, UserTokenRepository userTokenRepository,
                       SystemConfigRepository systemConfigRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.userTokenRepository = userTokenRepository;
        this.systemConfigRepository = systemConfigRepository;
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
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
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
        user.setIsApproved(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return toUserResponse(userRepository.save(user));
    }

    public List<OptionResponse> getRegisterRoleOptions() {
        return List.of(
                new OptionResponse("HorseOwner", "Chu ngua"),
                new OptionResponse("Jockey", "Ky si"),
                new OptionResponse("Referee", "Trong tai"),
                new OptionResponse("Spectator", "Khan gia"),
                new OptionResponse("Organizer", "Ban to chuc")
        );
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResponse login(LoginRequest request) {
        validateRequired(request.getUsername(), "username khong duoc de trong");
        validateRequired(request.getPassword(), "password khong duoc de trong");

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Sai username hoac password"));
        ensureSystemAvailableFor(user);

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
            throw new IllegalArgumentException("Sai mat khau. Con " + (5 - failedAttempts) + " lan thu.");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Spectator";
        String accessToken = jwtService.generateToken(user.getUserId(), user.getUsername(), roleName);
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
        ensureSystemAvailableFor(user);
        if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Tai khoan khong duoc phep refresh token");
        }

        storedToken.setIsRevoked(true);
        userTokenRepository.save(storedToken);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Spectator";
        String accessToken = jwtService.generateToken(user.getUserId(), user.getUsername(), roleName);
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

    public String forgotPassword(ForgotPasswordRequest request) {
        validateRequired(request.getEmail(), "email khong duoc de trong");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan voi email nay"));
        String resetToken = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendResetTokenEmail(user.getEmail(), resetToken);
        return "Ma xac nhan da duoc gui den email cua ban";
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
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("roleId khong ton tai"));
            if ("Admin".equalsIgnoreCase(role.getRoleName())) {
                throw new IllegalArgumentException("Khong duoc dang ky truc tiep role Admin");
            }
            return role;
        }

        String roleName = request.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            roleName = "Spectator";
        }
        roleName = normalizeRoleName(roleName);
        if ("Admin".equalsIgnoreCase(roleName)) {
            throw new IllegalArgumentException("Khong duoc dang ky truc tiep role Admin");
        }

        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("roleName khong ton tai"));
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "horseowner", "owner", "chu ngua" -> "HorseOwner";
            case "jockey", "nai ngua", "ky si" -> "Jockey";
            case "referee", "trong tai" -> "Referee";
            case "spectator", "khan gia" -> "Spectator";
            case "organizer", "organizerhead", "organizermember",
                    "truong ban to chuc", "thanh vien ban to chuc", "ban to chuc" -> "Organizer";
            case "admin" -> "Admin";
            default -> roleName.trim();
        };
    }

    private void ensureSystemAvailableFor(User user) {
        String roleName = user.getRole() == null ? null : user.getRole().getRoleName();
        if ("Admin".equalsIgnoreCase(roleName)) {
            return;
        }

        boolean maintenanceEnabled = systemConfigRepository.findByConfigKey("MAINTENANCE_MODE")
                .map(config -> "1".equals(config.getConfigValue()) || "true".equalsIgnoreCase(config.getConfigValue()))
                .orElse(false);
        if (!maintenanceEnabled) {
            return;
        }

        String until = systemConfigRepository.findByConfigKey("MAINTENANCE_UNTIL")
                .map(config -> config.getConfigValue() == null ? "" : config.getConfigValue().trim())
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (until == null) {
            throw new IllegalArgumentException("He thong dang bao tri. Chi admin duoc phep dang nhap.");
        }
        throw new IllegalArgumentException("He thong dang bao tri. Du kien mo lai vao " + until
                + ". Chi admin duoc phep dang nhap.");
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
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
