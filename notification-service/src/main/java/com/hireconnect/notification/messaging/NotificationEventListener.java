package com.hireconnect.notification.messaging;

import com.hireconnect.notification.config.RabbitMQConfig;
import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes notification events from all three RabbitMQ queues and delegates
 * to NotificationService for in-app persistence + optional email dispatch.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── Application status changes ─────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.APPLICATION_QUEUE)
    public void handleApplicationEvent(NotificationEvent event) {
        log.info("Received APPLICATION event for userId={}", event.getUserId());
        process(event);
    }

    // ── Interview scheduling ───────────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_QUEUE)
    public void handleInterviewEvent(NotificationEvent event) {
        log.info("Received INTERVIEW event for userId={}", event.getUserId());
        process(event);
    }

    // ── Job alerts ─────────────────────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.JOB_ALERT_QUEUE)
    public void handleJobAlertEvent(NotificationEvent event) {
        log.info("Received JOB_ALERT event for userId={}", event.getUserId());
        process(event);
    }

    // ── Shared dispatch logic ──────────────────────────────────────────────
    private void process(NotificationEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Received null or incomplete notification event — skipping");
            return;
        }
        try {
            notificationService.dispatchNotification(
                    event.getUserId(),
                    event.getType(),
                    event.getMessage(),
                    event.getEmail(),
                    event.getSubject()
            );
        } catch (Exception ex) {
            log.error("Failed to process notification event {}: {}", event, ex.getMessage(), ex);
            // Do NOT rethrow — prevents message from going back to queue in a loop
        }
    }
}
