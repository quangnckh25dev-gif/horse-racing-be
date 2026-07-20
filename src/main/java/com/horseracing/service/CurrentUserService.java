package com.horseracing.service;

import com.horseracing.entity.User;
import com.horseracing.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims;
            try {
                claims = jwtService.extractClaims(token);
            } catch (JwtException | IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid or expired token. Please log in again.");
            }
            Integer userId = claims.get("userId", Integer.class);
            if (userId != null) {
                return userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Current user was not found."));
            }

            return userRepository.findByUsername(claims.getSubject())
                    .orElseThrow(() -> new IllegalArgumentException("Current user was not found."));
        }

        throw new IllegalArgumentException("Authorization Bearer token is required.");
    }

    public boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && "Admin".equalsIgnoreCase(user.getRole().getRoleName());
    }
}
