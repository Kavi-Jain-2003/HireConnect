package com.hireconnect.notification.dto;

import java.io.Serializable;

/**
 * Generic event payload published to RabbitMQ by application-service,
 * interview-service, and job-service.
 *
 * Producers (application-service / interview-service / job-service) must
 * have an identical copy of this class in their own dto package.
 */
public class NotificationEvent implements Serializable {

    private Long   userId;      // recipient's userId
    private String type;        // APPLICATION_STATUS | INTERVIEW | JOB_ALERT
    private String message;     // human-readable body
    private String email;       // nullable — if set, email is also sent
    private String subject;     // email subject line (ignored for in-app only)

    // ── constructors ──────────────────────────────────────────────────────

    public NotificationEvent() {}

    public NotificationEvent(Long userId, String type, String message,
                              String email, String subject) {
        this.userId  = userId;
        this.type    = type;
        this.message = message;
        this.email   = email;
        this.subject = subject;
    }

    // ── static factory helpers ─────────────────────────────────────────────

    public static NotificationEvent applicationStatus(Long userId, String status,
                                                       String email) {
        String msg = "Your application status has been updated to: " + status;
        return new NotificationEvent(userId, "APPLICATION_STATUS", msg, email,
                "HireConnect — Application Update");
    }

    public static NotificationEvent interviewScheduled(Long userId, String details,
                                                        String email) {
        String msg = "Interview scheduled: " + details;
        return new NotificationEvent(userId, "INTERVIEW", msg, email,
                "HireConnect — Interview Invitation");
    }

    public static NotificationEvent jobAlert(Long userId, String jobTitle) {
        String msg = "New job matching your profile: " + jobTitle;
        return new NotificationEvent(userId, "JOB_ALERT", msg, null, null);
    }

    // ── getters / setters ──────────────────────────────────────────────────

    public Long getUserId()               { return userId; }
    public void setUserId(Long userId)    { this.userId = userId; }

    public String getType()               { return type; }
    public void setType(String type)      { this.type = type; }

    public String getMessage()            { return message; }
    public void setMessage(String message){ this.message = message; }

    public String getEmail()              { return email; }
    public void setEmail(String email)    { this.email = email; }

    public String getSubject()            { return subject; }
    public void setSubject(String subject){ this.subject = subject; }

    @Override
    public String toString() {
        return "NotificationEvent{userId=" + userId + ", type='" + type
                + "', email='" + email + "'}";
    }
}
