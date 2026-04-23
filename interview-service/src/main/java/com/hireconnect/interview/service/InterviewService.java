package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewService {

    Interview scheduleInterview(Interview interview);

    Interview confirmInterview(int interviewId);

    Interview rescheduleInterview(int interviewId, LocalDateTime newTime);

    void cancelInterview(int interviewId);

    List<Interview> getByApplication(int applicationId);

    List<Interview> getByStatus(String status);
}
