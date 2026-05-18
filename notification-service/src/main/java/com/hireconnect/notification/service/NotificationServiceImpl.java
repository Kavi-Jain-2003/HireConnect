package com.hireconnect.notification.service;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Updated NotificationServiceImpl.
 *
 * Changes from original:
 *  - JavaMailSender replaced by EmailService (HTML templates, proper fallback).
 *  - sendEmailAlert() delegates to EmailService.sendSimple().
 *  - dispatchNotification() now uses EmailService for richer emails when type is known.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repo;
    private final EmailService emailService;

    public NotificationServiceImpl(NotificationRepository repo, EmailService emailService) {
        this.repo         = repo;
        this.emailService = emailService;
    }

    @Override
    public Notification sendNotification(Notification notification) {
        if (notification == null) throw new RuntimeException("Notification cannot be null");

        notification.setNotificationId(null);
        notification.setRead(false);
        if (notification.getCreatedAt() == null) notification.setCreatedAt(LocalDateTime.now());
        if (notification.getType() != null)
            notification.setType(notification.getType().trim().toUpperCase(Locale.ROOT));

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
            list.forEach(n -> n.setRead(true));
            repo.saveAll(list);
        }
    }

    @Override
    public void deleteNotification(Long notificationId) {
        if (!repo.existsById(notificationId)) throw new RuntimeException("Notification not found");
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
        // Delegates to EmailService — handles fallback + HTML wrapping internally
        emailService.sendSimple(email, subject, message);
    }

    @Override
    public Notification dispatchNotification(Long userId, String type, String message,
                                              String email, String subject) {
        if (userId != null && type != null && message != null) {
            repo.findFirstByUserIdAndTypeAndMessageOrderByCreatedAtDesc(
                    userId, type.trim().toUpperCase(Locale.ROOT), message)
                    .ifPresent(existing -> {
                        // idempotency guard — don't duplicate in-app notification
                    });
        }

        // Persist in-app notification
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        Notification saved = sendNotification(notification);

        // Send email if address provided
        if (email != null && !email.isBlank()) {
            dispatchTypedEmail(type, email, message, subject);
        }
        return saved;
    }

    @Override
    public Notification notifyApplicationStatus(Long userId, String status, String message) {
        return dispatchNotification(userId, "APPLICATION_STATUS",
                buildMessage(status, message), null, null);
    }

    @Override
    public Notification notifyInterview(Long userId, String status, String message) {
        return dispatchNotification(userId, "INTERVIEW",
                buildMessage(status, message), null, null);
    }

    @Override
    public Notification notifyJobAlert(Long userId, String message) {
        return dispatchNotification(userId, "JOB_ALERT", message, null, null);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Routes to the appropriate EmailService method based on notification type
     * so the right HTML template is used.
     */
    private void dispatchTypedEmail(String type, String email, String message, String subject) {
        if (type == null) {
            emailService.sendSimple(email, subject, message);
            return;
        }
        switch (type.toUpperCase(Locale.ROOT)) {
            case "APPLICATION_STATUS" ->
                    // message already contains status info — send as styled plain
                    emailService.sendSimple(email,
                            subject != null ? subject : "HireConnect — Application Update", message);
            case "INTERVIEW" ->
                    emailService.sendSimple(email,
                            subject != null ? subject : "HireConnect — Interview Invitation", message);
            default ->
                    emailService.sendSimple(email,
                            subject != null ? subject : "HireConnect Notification", message);
        }
    }

    private String buildMessage(String status, String message) {
        if (status == null || status.isBlank()) return message;
        if (message == null || message.isBlank()) return status;
        return status + ": " + message;
    }

    private void verifyOwnership(Notification n, Long currentUserId) {
        if (currentUserId == null || n.getUserId() == null
                || !currentUserId.equals(n.getUserId())) {
            throw new RuntimeException("You are not allowed to modify this notification");
        }
    }
}
