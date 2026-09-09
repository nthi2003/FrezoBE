package com.frezo.common.service;

import com.frezo.common.entity.Notification;
import com.frezo.common.response.PageResponse;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    void sendToTopic(String topic, Object payload);

    void sendToUser(String username, String destination, Object payload);


    void notifyUserWithEmailFallback(String username, String title, String message, boolean urgent);


    void notify(String username, String title, String message,
                String type, String entityType, String entityId,
                String actionUrl, String senderUsername, boolean urgent);


    void notifyMany(List<String> usernames, String title, String message,
                    String type, String entityType, String entityId,
                    String actionUrl, String senderUsername, boolean urgent);


    List<Notification> getMyNotifications(String username);


    PageResponse<Notification> getMyNotifications(String username, Integer page, Integer size,
                                                   String tab, String type, String search);

    void markAsRead(String notificationId);

    /** v1.2 — mark all unread as read for user. */
    int markAllAsRead(String username);

    /** v1.2 — unread count cho badge. */
    long getUnreadCount(String username);

    /** Tổng số thông báo của user (không filter). */
    long getTotalCount(String username);

    /** Số thông báo khẩn chưa đọc. */
    long getUrgentUnreadCount(String username);

    /** Tổng hợp unread/total/urgent cho badge + stats trang. */
    Map<String, Long> getNotificationSummary(String username);
}
