package com.frezo.auth.repository;

import com.frezo.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {

    Optional<TokenBlacklist> findByToken(String token);

    boolean existsByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}

