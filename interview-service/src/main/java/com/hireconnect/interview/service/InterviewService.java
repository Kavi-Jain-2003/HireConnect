package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewService {

    Interview scheduleInterview(Interview interview);

    Interview confirmInterview(Long interviewId);       // was int

    Interview rescheduleInterview(Long interviewId, LocalDateTime newTime); // was int

    void cancelInterview(Long interviewId);             // was int

    List<Interview> getByApplication(Long applicationId); // was int

    List<Interview> getByStatus(String status);
}