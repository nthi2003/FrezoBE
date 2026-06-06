package com.frezo.common.repository;

import com.frezo.common.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);
    long countByUsernameAndIsReadFalse(String username);
}
