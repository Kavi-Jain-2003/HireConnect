package com.hireconnect.interview.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Publishes interview notification events to RabbitMQ from interview-service.
 *
 * The existing NotificationClient (Feign) can remain as a fallback.
 * Swap the call in InterviewServiceImpl from notificationClient.dispatch(...)
 * to eventPublisher.publishInterviewScheduled(...) for async delivery.
 */
@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);
    private static final String EXCHANGE = "hireconnect.events";

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public NotificationEventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    /**
     * @param userId       candidate's userId
     * @param details      interview details string (date, mode, link)
     * @param email        candidate email for sending the invitation
     */
    public void publishInterviewScheduled(Long userId, String details, String email) {
        NotificationEvent event = new NotificationEvent(
                userId,
                "INTERVIEW",
                "Interview scheduled: " + details,
                email,
                "HireConnect — Interview Invitation"
        );
        publish("interview.scheduled", event);
    }

    public void publishInterviewCancelled(Long userId, String jobTitle, String email) {
        NotificationEvent event = new NotificationEvent(
                userId,
                "INTERVIEW",
                "Your interview for \"" + jobTitle + "\" has been cancelled.",
                email,
                "HireConnect — Interview Cancelled"
        );
        publish("interview.scheduled", event); // same queue, different message
    }

    private void publish(String routingKey, NotificationEvent event) {
        RabbitTemplate template = rabbitTemplateProvider.getIfAvailable();
        if (template == null) {
            log.warn("RabbitMQ not available — interview notification dropped: {}", event);
            return;
        }
        try {
            template.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published interview event → userId={}", event.getUserId());
        } catch (Exception ex) {
            log.error("Failed to publish interview event: {}", ex.getMessage(), ex);
        }
    }
}
