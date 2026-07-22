package com.frezo.common.service;

import com.frezo.common.entity.Notification;
import java.util.List;

public interface NotificationService {

    void sendToTopic(String topic, Object payload);

    void sendToUser(String username, String destination, Object payload);

    /** Legacy — vẫn giữ cho backward compat, không có deep-link. */
    void notifyUserWithEmailFallback(String username, String title, String message, boolean urgent);

    /**
     * Gửi thông báo domain event kèm deep-link. Ghi DB + push WS + email nếu {@code urgent}.
     *
     * @param username        recipient
     * @param title           tiêu đề ngắn (VD: "Bạn có ticket mới")
     * @param message         nội dung (VD: "AN đã giao ticket #TICKET-A1B2 cho bạn")
     * @param type            loại (VD: "TICKET_ASSIGNED") — FE map sang icon
     * @param entityType      "TICKET" | "PAYROLL" | ...
     * @param entityId        id entity liên quan
     * @param actionUrl       FE navigate URL khi click
     * @param senderUsername  username người trigger (có thể null cho system event)
     * @param urgent          true ⇒ priority=URGENT + gửi email
     */
    void notify(String username, String title, String message,
                String type, String entityType, String entityId,
                String actionUrl, String senderUsername, boolean urgent);

    /** Bulk fire-and-forget — skip null/empty username, tự khử trùng lặp. */
    void notifyMany(List<String> usernames, String title, String message,
                    String type, String entityType, String entityId,
                    String actionUrl, String senderUsername, boolean urgent);

    List<Notification> getMyNotifications(String username);

    void markAsRead(String notificationId);

    /** v1.2 — mark all unread as read for user. */
    int markAllAsRead(String username);

    /** v1.2 — unread count cho badge. */
    long getUnreadCount(String username);
}
