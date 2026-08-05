package com.frezo.server.service;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.entity.Notification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.repository.NotificationRepository;
import com.frezo.common.response.PageResponse;
import com.frezo.common.service.NotificationService;
import com.frezo.email.service.EmailService;
import com.frezo.qtht.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final NotificationRepository notificationRepository;
    /** ObjectProvider để tránh circular (PushNotificationService cũng ở downstream module). */
    private final ObjectProvider<PushNotificationService> pushServiceProvider;

    // ==========================================================
    // Raw messaging primitives
    // ==========================================================

    @Override
    public void sendToTopic(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
        } catch (Exception e) {
            log.warn("WS send to topic {} failed: {}", topic, e.getMessage());
        }
    }

    @Override
    public void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
        } catch (Exception e) {
            log.warn("WS send to user {} failed: {}", username, e.getMessage());
        }
    }

    // ==========================================================
    // Legacy — kept for backward compat
    // ==========================================================

    @Override
    public void notifyUserWithEmailFallback(String username, String title, String message, boolean urgent) {
        notify(username, title, message,
                urgent ? "WARNING" : "INFO",
                null, null, null, null, urgent);
    }

    // ==========================================================
    // Rich domain notification (v1.2)
    // ==========================================================

    @Override
    @Transactional
    public void notify(String username, String title, String message,
                       String type, String entityType, String entityId,
                       String actionUrl, String senderUsername, boolean urgent) {
        if (username == null || username.isBlank()) return;
        // Không tự thông báo cho chính mình
        if (senderUsername != null && senderUsername.equalsIgnoreCase(username)) return;

        Notification n = Notification.builder()
                .username(username)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .type(type != null ? type : "INFO")
                .actionUrl(actionUrl)
                .entityType(entityType)
                .entityId(entityId)
                .senderUsername(senderUsername)
                .priority(urgent ? "URGENT" : "NORMAL")
                .build();
        notificationRepository.save(n);

        // Push realtime qua WebSocket — FE nào có kết nối sẽ nhận ngay
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", n.getId());
        payload.put("title", title);
        payload.put("message", message);
        payload.put("type", n.getType());
        payload.put("entityType", entityType);
        payload.put("entityId", entityId);
        payload.put("actionUrl", actionUrl);
        payload.put("link", actionUrl);
        payload.put("senderUsername", senderUsername);
        payload.put("priority", n.getPriority());
        payload.put("createdAt", n.getCreatedAt());
        sendToUser(username, "/queue/notifications", payload);

        if (urgent) {
            trySendUrgentEmail(username, title, message);
        }

        // Mobile push (fire-and-forget) — ObjectProvider vì bean có thể null trong test context.
        try {
            PushNotificationService ps = pushServiceProvider.getIfAvailable();
            if (ps != null) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("type", n.getType());
                if (entityType != null) data.put("entityType", entityType);
                if (entityId != null)   data.put("entityId", entityId);
                if (actionUrl != null)  data.put("href", actionUrl);
                data.put("notificationId", n.getId());
                ps.sendToUser(username, title, message, data);
            }
        } catch (Exception e) {
            log.warn("Push notification failed for user {}: {}", username, e.getMessage());
        }
    }

    @Override
    public void notifyMany(List<String> usernames, String title, String message,
                           String type, String entityType, String entityId,
                           String actionUrl, String senderUsername, boolean urgent) {
        if (usernames == null || usernames.isEmpty()) return;
        // Khử trùng lặp + bỏ null
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String u : usernames) {
            if (u != null && !u.isBlank()) uniq.add(u.trim());
        }
        for (String u : uniq) {
            try {
                notify(u, title, message, type, entityType, entityId, actionUrl, senderUsername, urgent);
            } catch (Exception e) {
                log.warn("notifyMany failed for user {}: {}", u, e.getMessage());
            }
        }
    }

    private void trySendUrgentEmail(String username, String title, String message) {
        try {
            Optional<User> userOpt = userRepository.findByUserName(username);
            if (userOpt.isEmpty() || userOpt.get().getEmail() == null || userOpt.get().getEmail().isBlank()) {
                log.warn("Could not find email for user: {}. Fallback email not sent.", username);
                return;
            }
            String actualEmail = userOpt.get().getEmail();
            try {
                emailService.sendByTemplate("URGENT_NOTIFICATION",
                        Map.of("title", title, "content", message),
                        Collections.singletonList(actualEmail));
                log.info("Sent fallback email (template) to: {}", actualEmail);
            } catch (Exception templateEx) {
                // Template chưa seed / SMTP lỗi template → gửi HTML thô
                String html = "<div style=\"font-family:sans-serif;max-width:560px\">"
                        + "<h2 style=\"color:#059669\">" + escapeHtml(title) + "</h2>"
                        + "<p style=\"white-space:pre-line;line-height:1.6\">" + escapeHtml(message) + "</p>"
                        + "<p style=\"color:#94a3b8;font-size:12px\">Frezo ERP</p></div>";
                emailService.sendSimple(actualEmail, title, html);
                log.info("Sent fallback email (raw) to: {} — template error: {}", actualEmail, templateEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send fallback email to {}", username, e);
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ==========================================================
    // Reads
    // ==========================================================

    @Override
    public List<Notification> getMyNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Notification> getMyNotifications(String username, Integer page, Integer size,
                                                         String tab, String type, String search) {
        if (username == null || username.isBlank()) {
            int p = page == null ? 0 : Math.max(page - 1, 0);
            int s = size == null || size <= 0 ? 20 : size;
            return PageResponse.empty(p, s);
        }

        Pageable pageable = ServiceHelper.createPageable(
                page != null ? page : 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Notification> spec = buildMyNotificationsSpec(username, tab, type, search);
        Page<Notification> result = notificationRepository.findAll(spec, pageable);
        return PageResponse.from(result);
    }

    private static Specification<Notification> buildMyNotificationsSpec(
            String username, String tab, String type, String search) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("username"), username));

            String tabKey = tab == null ? "all" : tab.trim().toLowerCase();
            if ("unread".equals(tabKey)) {
                predicates.add(cb.or(
                        cb.isFalse(root.get("isRead")),
                        cb.isNull(root.get("isRead"))));
            } else if ("urgent".equals(tabKey)) {
                predicates.add(cb.equal(cb.upper(root.get("priority")), "URGENT"));
            }

            if (StringUtils.hasText(type) && !"ALL".equalsIgnoreCase(type.trim())) {
                predicates.add(cb.equal(root.get("type"), type.trim()));
            }

            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("title"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("message"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("senderUsername"), "")), like)));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public int markAllAsRead(String username) {
        if (username == null || username.isBlank()) return 0;
        return notificationRepository.markAllReadByUsername(username);
    }

    @Override
    public long getUnreadCount(String username) {
        if (username == null || username.isBlank()) return 0;
        return notificationRepository.countByUsernameAndIsReadFalse(username);
    }

    @Override
    public long getTotalCount(String username) {
        if (username == null || username.isBlank()) return 0;
        return notificationRepository.countByUsername(username);
    }

    @Override
    public long getUrgentUnreadCount(String username) {
        if (username == null || username.isBlank()) return 0;
        return notificationRepository.countByUsernameAndPriorityAndIsReadFalse(username, "URGENT");
    }
}
