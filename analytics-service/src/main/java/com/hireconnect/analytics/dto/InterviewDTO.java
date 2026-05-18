package com.hireconnect.analytics.dto;

import java.time.LocalDateTime;

public class InterviewDTO {

    private Long interviewId;
    private Long applicationId;
    private LocalDateTime scheduledAt;
    private String status;

    public InterviewDTO() {}

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
