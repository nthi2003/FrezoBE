package com.frezo.common.repository;

import com.frezo.common.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String>, JpaSpecificationExecutor<Notification> {

    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);

    Page<Notification> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    long countByUsername(String username);

    long countByUsernameAndIsReadFalse(String username);

    long countByUsernameAndPriorityAndIsReadFalse(String username, String priority);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.username = :username AND n.isRead = false")
    int markAllReadByUsername(@Param("username") String username);

    boolean existsByUsernameAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqual(
            String username, String entityType, String entityId, LocalDateTime createdAt);

    long countByUsernameAndEntityTypeAndCreatedAtGreaterThanEqual(
            String username, String entityType, LocalDateTime createdAt);
}
