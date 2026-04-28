package com.hireconnect.notification.service;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.repository.NotificationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public NotificationServiceImpl(NotificationRepository repo, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.repo = repo;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public Notification sendNotification(Notification notification) {
        if (notification == null) {
            throw new RuntimeException("Notification cannot be null");
        }

        notification.setNotificationId(null);
        notification.setRead(false);
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getType() != null) {
            notification.setType(notification.getType().trim().toUpperCase(Locale.ROOT));
        }
        return repo.save(notification);
    }

    @Override
    public List<Notification> getByUser(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        n.setRead(true);
        repo.save(n);
    }

    @Override
    public void markAsRead(Long notificationId, Long currentUserId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        verifyOwnership(n, currentUserId);
        n.setRead(true);
        repo.save(n);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Notification> list = repo.findByUserIdAndIsRead(userId, false);
        if (!list.isEmpty()) {
            for (Notification n : list) {
                n.setRead(true);
            }
            repo.saveAll(list);
        }
    }

    @Override
    public void deleteNotification(Long notificationId) {
        if (!repo.existsById(notificationId)) {
            throw new RuntimeException("Notification not found");
        }
        repo.deleteByNotificationId(notificationId);
    }

    @Override
    public void deleteNotification(Long notificationId, Long currentUserId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        verifyOwnership(n, currentUserId);
        repo.deleteByNotificationId(notificationId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return repo.countByUserIdAndIsRead(userId, false);
    }

    @Override
    public void sendEmailAlert(String email, String subject, String message) {
        if (email == null || email.isBlank()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            System.out.println("EMAIL SENT TO: " + email);
            System.out.println("SUBJECT: " + subject);
            System.out.println("MESSAGE: " + message);
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(email);
            mail.setSubject(subject == null || subject.isBlank() ? "HireConnect Notification" : subject);
            mail.setText(message == null ? "" : message);
            mailSender.send(mail);
        } catch (Exception ex) {
            System.out.println("EMAIL DELIVERY FAILED FOR: " + email);
            System.out.println("SUBJECT: " + subject);
            System.out.println("MESSAGE: " + message);
        }
    }

    @Override
    public Notification dispatchNotification(Long userId, String type, String message, String email, String subject) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);

        Notification saved = sendNotification(notification);
        if (email != null && !email.isBlank()) {
            sendEmailAlert(email, subject, message);
        }
        return saved;
    }

    @Override
    public Notification notifyApplicationStatus(Long userId, String status, String message) {
        return dispatchNotification(userId, "APPLICATION_STATUS", buildMessage(status, message), null, null);
    }

    @Override
    public Notification notifyInterview(Long userId, String status, String message) {
        return dispatchNotification(userId, "INTERVIEW", buildMessage(status, message), null, null);
    }

    @Override
    public Notification notifyJobAlert(Long userId, String message) {
        return dispatchNotification(userId, "JOB_ALERT", message, null, null);
    }

    private String buildMessage(String status, String message) {
        if (status == null || status.isBlank()) {
            return message;
        }
        if (message == null || message.isBlank()) {
            return status;
        }
        return status + ": " + message;
    }

    private void verifyOwnership(Notification notification, Long currentUserId) {
        if (currentUserId == null || notification.getUserId() == null || !currentUserId.equals(notification.getUserId())) {
            throw new RuntimeException("You are not allowed to modify this notification");
        }
    }
}
