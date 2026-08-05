package com.frezo.auth.repository;

import com.frezo.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByIsActiveTrue();

    Page<UserSession> findByIsActiveTrue(Pageable pageable);

    List<UserSession> findByIsActiveTrueAndLastActiveTimeAfter(LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT s.username) FROM UserSession s WHERE s.isActive = true AND s.lastActiveTime >= :since")
    long countDistinctOnlineUsers(@Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserSession s SET s.lastActiveTime = :now WHERE s.token = :token AND s.isActive = true")
    int touchByToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserSession s SET s.lastActiveTime = :now WHERE s.username = :username AND s.isActive = true")
    int touchByUsername(@Param("username") String username, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserSession s SET s.token = :token WHERE s.username = :username AND s.isActive = true")
    int updateTokenByUsername(@Param("username") String username, @Param("token") String token);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
