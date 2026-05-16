package com.hireconnect.application.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Publishes notification events to RabbitMQ from application-service.
 *
 * Falls back silently if RabbitMQ is unavailable, so the application-service
 * continues working even without a broker (e.g., local dev without Docker).
 *
 * Routing keys must match the bindings declared in
 * notification-service's RabbitMQConfig.
 */
@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    // Must match RabbitMQConfig.EXCHANGE in notification-service
    private static final String EXCHANGE = "hireconnect.events";

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public NotificationEventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    /**
     * Publish an application-status-changed event.
     *
     * @param userId     recipient user id
     * @param status     new status string e.g. "SHORTLISTED"
     * @param email      candidate email (nullable — omit to skip email)
     */
    public void publishApplicationStatusChanged(Long userId, String status, String email) {
        NotificationEvent event = NotificationEvent.applicationStatus(userId, status, email);
        publish("application.status.changed", event);
    }

    /**
     * Publish an interview-scheduled event.
     *
     * @param userId      candidate user id
     * @param details     human-readable interview details
     * @param email       candidate email
     */
    public void publishInterviewScheduled(Long userId, String details, String email) {
        NotificationEvent event = NotificationEvent.interviewScheduled(userId, details, email);
        publish("interview.scheduled", event);
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private void publish(String routingKey, NotificationEvent event) {
        RabbitTemplate template = rabbitTemplateProvider.getIfAvailable();
        if (template == null) {
            log.warn("RabbitMQ not available — notification event dropped: {}", event);
            return;
        }
        try {
            template.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published event → exchange={} routingKey={} userId={}",
                    EXCHANGE, routingKey, event.getUserId());
        } catch (Exception ex) {
            log.error("Failed to publish notification event: {}", ex.getMessage(), ex);
            // Non-fatal: main flow (application submission) must not fail
        }
    }
}
