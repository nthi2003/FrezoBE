package com.frezo.server.service;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.entity.Notification;
import com.frezo.common.service.NotificationService;
import com.frezo.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final com.frezo.common.repository.NotificationRepository notificationRepository;

    @Override
    public void sendToTopic(String topic, Object payload) {
        messagingTemplate.convertAndSend(topic, payload);
    }

    @Override
    public void sendToUser(String username, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(username, destination, payload);
    }

    @Override
    public void notifyUserWithEmailFallback(String username, String title, String message, boolean urgent) {
        // Save to DB for Notification Center
        com.frezo.common.entity.Notification notification = com.frezo.common.entity.Notification.builder()
                .username(username)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(java.time.LocalDateTime.now())
                .type(urgent ? "WARNING" : "INFO")
                .build();
        notificationRepository.save(notification);

        // WebSocket push
        sendToUser(username, "/queue/notifications", Map.of(
                "id", notification.getId(),
                "title", title,
                "message", message,
                "createdAt", notification.getCreatedAt()));

        if (urgent) {
            try {
                Optional<User> userOpt = userRepository.findByUserName(username);
                if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
                    String actualEmail = userOpt.get().getEmail();
                    emailService.sendByTemplate("URGENT_NOTIFICATION",
                            Map.of("title", title, "content", message),
                            Collections.singletonList(actualEmail));
                    log.info("Sent fallback email to actual address: {}", actualEmail);
                } else {
                    log.warn("Could not find email for user: {}. Fallback email not sent.", username);
                }
            } catch (Exception e) {
                log.error("Failed to send fallback email to {}", username, e);
            }
        }
    }

    @Override
    public List<Notification> getMyNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }
}
