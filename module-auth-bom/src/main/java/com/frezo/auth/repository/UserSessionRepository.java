package com.frezo.auth.repository;

import com.frezo.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findByUsernameAndIsActiveTrue(String username);

    Page<UserSession> findByUsernameAndIsActiveTrue(String username, Pageable pageable);

    Optional<UserSession> findByToken(String token);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByUsernameAndIsActiveTrueAndIdNot(String username, String id);

    long countByUsernameAndIsActiveTrue(String username);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}


