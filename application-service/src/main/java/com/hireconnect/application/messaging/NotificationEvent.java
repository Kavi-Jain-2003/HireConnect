package com.hireconnect.application.messaging;

import java.io.Serializable;

/**
 * Event payload published to RabbitMQ.
 * Keep in sync with the identical class in notification-service.
 */
public class NotificationEvent implements Serializable {

    private Long   userId;
    private String type;
    private String message;
    private String email;
    private String subject;

    public NotificationEvent() {}

    public NotificationEvent(Long userId, String type, String message,
                              String email, String subject) {
        this.userId  = userId;
        this.type    = type;
        this.message = message;
        this.email   = email;
        this.subject = subject;
    }

    public static NotificationEvent applicationStatus(Long userId, String status, String email) {
        return new NotificationEvent(userId, "APPLICATION_STATUS",
                "Your application status has been updated to: " + status,
                email, "HireConnect — Application Update");
    }

    public static NotificationEvent interviewScheduled(Long userId, String details, String email) {
        return new NotificationEvent(userId, "INTERVIEW",
                "Interview scheduled: " + details,
                email, "HireConnect — Interview Invitation");
    }

    public Long   getUserId()                { return userId; }
    public void   setUserId(Long userId)     { this.userId = userId; }
    public String getType()                  { return type; }
    public void   setType(String type)       { this.type = type; }
    public String getMessage()               { return message; }
    public void   setMessage(String message) { this.message = message; }
    public String getEmail()                 { return email; }
    public void   setEmail(String email)     { this.email = email; }
    public String getSubject()               { return subject; }
    public void   setSubject(String subject) { this.subject = subject; }

    @Override
    public String toString() {
        return "NotificationEvent{userId=" + userId + ", type='" + type + "'}";
    }
}
