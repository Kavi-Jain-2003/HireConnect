package com.hireconnect.interview.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long interviewId;   // was int

    private Long applicationId; // was int
    private LocalDateTime scheduledAt;
    private String mode;        // Online / In-Person
    private String meetLink;
    private String location;
    private String status;      // SCHEDULED, CONFIRMED, RESCHEDULED, CANCELLED
    private String notes;

    public Interview() {}

    public Interview(Long applicationId, LocalDateTime scheduledAt, String mode,
                     String meetLink, String location, String status, String notes) {
        this.applicationId = applicationId;
        this.scheduledAt = scheduledAt;
        this.mode = mode;
        this.meetLink = meetLink;
        this.location = location;
        this.status = status;
        this.notes = notes;
    }

    // Getters & Setters

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getMeetLink() { return meetLink; }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}