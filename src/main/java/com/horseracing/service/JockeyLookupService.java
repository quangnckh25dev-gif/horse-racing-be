package com.horseracing.service;

import com.horseracing.dto.JockeyOptionResponse;
import com.horseracing.entity.Jockey;
import com.horseracing.entity.User;
import com.horseracing.repository.JockeyRepository;
import com.horseracing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JockeyLookupService {

    private final UserRepository userRepository;
    private final JockeyRepository jockeyRepository;

    public JockeyLookupService(UserRepository userRepository, JockeyRepository jockeyRepository) {
        this.userRepository = userRepository;
        this.jockeyRepository = jockeyRepository;
    }

    public List<JockeyOptionResponse> getActiveJockeys() {
        List<User> users = userRepository.findByRole_RoleNameAndIsActiveTrue("Jockey");
        return users.stream()
                .map(this::toJockeyOption)
                .filter(item -> item != null)
                .toList();
    }

    private JockeyOptionResponse toJockeyOption(User user) {
        Jockey jockey = jockeyRepository.findByUserId(user.getUserId()).orElse(null);
        if (jockey == null) {
            return null;
        }
        return new JockeyOptionResponse(
                jockey.getJockeyId(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                jockey.getTotalRaces(),
                jockey.getTotalWins()
        );
    }
}