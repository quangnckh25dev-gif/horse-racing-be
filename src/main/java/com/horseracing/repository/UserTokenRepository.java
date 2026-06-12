package com.horseracing.repository;

import com.horseracing.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {
    Optional<UserToken> findByToken(String token);

    List<UserToken> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
