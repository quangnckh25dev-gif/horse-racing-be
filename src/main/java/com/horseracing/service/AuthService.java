package com.horseracing.service;

import com.horseracing.dto.ChangePasswordRequest;
import com.horseracing.dto.ForgotPasswordRequest;
import com.horseracing.dto.GoogleLoginRequest;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserTokenRepository userTokenRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final WebClient googleWebClient;

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
        this.googleWebClient = WebClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();
    }

    public UserResponse register(RegisterRequest request) {
        validateRequired(request.getUsername(), "Username is required.");
        validateRequired(request.getPassword(), "Password is required.");
        validateRequired(request.getFullName(), "Full name is required.");
        validateRequired(request.getEmail(), "Email is required.");
        validateRequired(request.getPhone(), "Phone number is required.");

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists.");
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
        user.setIsApproved("Spectator".equalsIgnoreCase(role.getRoleName()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return toUserResponse(userRepository.save(user));
    }

    public List<OptionResponse> getRegisterRoleOptions() {
        return List.of(
                new OptionResponse("HorseOwner", "Horse Owner"),
                new OptionResponse("Jockey", "Jockey"),
                new OptionResponse("Referee", "Referee"),
                new OptionResponse("Spectator", "Spectator"),
                new OptionResponse("Organizer", "Organizer")
        );
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResponse login(LoginRequest request) {
        validateRequired(request.getUsername(), "Username is required.");
        validateRequired(request.getPassword(), "Password is required.");

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Account is not registered."));
        ensureSystemAvailableFor(user);

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new IllegalArgumentException("Account has been locked due to too many failed login attempts.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is inactive.");
        }
        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Account is waiting for admin approval.");
        }
        if (!matchesPassword(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
            failedAttempts++;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setIsLocked(true);
                user.setIsActive(false);
                userRepository.save(user);
                throw new IllegalArgumentException("Account has been locked due to too many failed login attempts.");
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Wrong password. " + (5 - failedAttempts) + " attempts remaining.");
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
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        validateRequired(request.getIdToken(), "Google idToken is required.");

        Map<String, Object> tokenInfo = verifyGoogleIdToken(request.getIdToken());
        String email = valueAsString(tokenInfo.get("email"));
        validateRequired(email, "Google account email is required.");

        String emailVerified = valueAsString(tokenInfo.get("email_verified"));
        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new IllegalArgumentException("Google account email is not verified.");
        }

        User user = userRepository.findByEmail(email)
                .map(existingUser -> validateGoogleSpectator(existingUser))
                .orElseGet(() -> createGoogleSpectator(tokenInfo));

        ensureSystemAvailableFor(user);
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is inactive.");
        }
        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Account is waiting for admin approval.");
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
        validateRequired(request.getRefreshToken(), "refreshToken is required.");

        UserToken storedToken = userTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
        if (Boolean.TRUE.equals(storedToken.getIsRevoked()) || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired or has been revoked.");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Token user was not found."));
        ensureSystemAvailableFor(user);
        if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalArgumentException("Account is not allowed to refresh token.");
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
        validateRequired(request.getRefreshToken(), "refreshToken is required.");
        UserToken storedToken = userTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
        storedToken.setIsRevoked(true);
        userTokenRepository.save(storedToken);
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        validateRequired(request.getEmail(), "Email is required.");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account was found for this email."));
        String resetToken = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendResetTokenEmail(user.getEmail(), resetToken);
        return "Verification code has been sent to your email";
    }

    public UserResponse resetPasswordWithToken(ResetPasswordRequest request) {
        validateRequired(request.getToken(), "token is required.");
        validateRequired(request.getNewPassword(), "newPassword is required.");

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token or token was not found."));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        return toUserResponse(userRepository.save(user));
    }

    public UserResponse changePassword(String username, ChangePasswordRequest request) {
        validateRequired(request.getOldPassword(), "oldPassword is required.");
        validateRequired(request.getNewPassword(), "newPassword is required.");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!matchesPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        return toUserResponse(userRepository.save(user));
    }

    private Role resolveRole(RegisterRequest request) {
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("roleId does not exist."));
            if ("Admin".equalsIgnoreCase(role.getRoleName())) {
                throw new IllegalArgumentException("Admin role cannot be registered directly.");
            }
            return role;
        }

        String roleName = request.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        roleName = normalizeRoleName(roleName);
        if ("Admin".equalsIgnoreCase(roleName)) {
            throw new IllegalArgumentException("Admin role cannot be registered directly.");
        }

        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("roleName does not exist."));
    }

    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        try {
            Map<String, Object> tokenInfo = googleWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
            if (tokenInfo == null || tokenInfo.containsKey("error")) {
                throw new IllegalArgumentException("Invalid Google token.");
            }
            return tokenInfo;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Google token.");
        }
    }

    private User validateGoogleSpectator(User user) {
        String roleName = user.getRole() == null ? null : user.getRole().getRoleName();
        if (!"Spectator".equalsIgnoreCase(roleName)) {
            throw new IllegalArgumentException("This email is already registered with another role.");
        }
        return user;
    }

    private User createGoogleSpectator(Map<String, Object> tokenInfo) {
        Role spectatorRole = roleRepository.findByRoleName("Spectator")
                .orElseThrow(() -> new IllegalArgumentException("Spectator role does not exist."));
        String email = valueAsString(tokenInfo.get("email"));
        String fullName = valueAsString(tokenInfo.get("name"));
        if (fullName == null || fullName.isBlank()) {
            fullName = email;
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(generateGoogleUsername(email));
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setAvatarUrl(valueAsString(tokenInfo.get("picture")));
        user.setRole(spectatorRole);
        user.setIsActive(true);
        user.setIsApproved(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private String generateGoogleUsername(String email) {
        String baseUsername = email.split("@")[0]
                .replaceAll("[^A-Za-z0-9_]", "")
                .toLowerCase(Locale.ROOT);
        if (baseUsername.isBlank()) {
            baseUsername = "spectator";
        }

        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }
        return username;
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
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
            throw new IllegalArgumentException("The system is under maintenance. Only admins can log in.");
        }
        throw new IllegalArgumentException("The system is under maintenance. Expected reopening time: " + until
                + ". Only admins can log in.");
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
