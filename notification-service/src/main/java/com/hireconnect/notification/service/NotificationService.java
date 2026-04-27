package com.hireconnect.notification.service;

import com.hireconnect.notification.entity.Notification;
import java.util.List;

public interface NotificationService {

    Notification sendNotification(Notification notification);

    List<Notification> getByUser(Long userId);

    void markAsRead(Long notificationId);

    void markAllRead(Long userId);

    void deleteNotification(Long notificationId);

    long getUnreadCount(Long userId);

    void sendEmailAlert(String email, String subject, String message);

    Notification dispatchNotification(Long userId, String type, String message, String email, String subject);

    Notification notifyApplicationStatus(Long userId, String status, String message);

    Notification notifyInterview(Long userId, String status, String message);

    Notification notifyJobAlert(Long userId, String message);
}
