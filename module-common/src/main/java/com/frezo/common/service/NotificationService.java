package com.frezo.common.service;

import com.frezo.common.entity.Notification;
import java.util.List;

public interface NotificationService {
    void sendToTopic(String topic, Object payload);
    void sendToUser(String username, String destination, Object payload);
    void notifyUserWithEmailFallback(String username, String title, String message, boolean urgent);
    List<Notification> getMyNotifications(String username);
    void markAsRead(String notificationId);
}
