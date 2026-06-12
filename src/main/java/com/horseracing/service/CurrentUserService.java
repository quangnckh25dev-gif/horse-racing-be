package com.horseracing.service;

import com.horseracing.entity.User;
import com.horseracing.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public CurrentUserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public User getCurrentUser(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Integer userId = Integer.valueOf(userIdHeader);
                return userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user hien tai"));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("X-User-Id khong hop le");
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtService.extractClaims(token);
            return userRepository.findByUsername(claims.getSubject())
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user hien tai"));
        }

        throw new IllegalArgumentException("Thieu thong tin user hien tai");
    }

    public boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && "Admin".equalsIgnoreCase(user.getRole().getRoleName());
    }
}
