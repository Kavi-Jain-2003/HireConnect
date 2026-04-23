package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.repository.InterviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewServiceImpl implements InterviewService {

    @Autowired
    private InterviewRepository repository;

    @Override
    public Interview scheduleInterview(Interview interview) {
        interview.setStatus("SCHEDULED");
        return repository.save(interview);
    }

    @Override
    public Interview confirmInterview(int interviewId) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setStatus("CONFIRMED");
        return repository.save(interview);
    }

    @Override
    public Interview rescheduleInterview(int interviewId, LocalDateTime newTime) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setScheduledAt(newTime);
        interview.setStatus("RESCHEDULED");
        return repository.save(interview);
    }

    @Override
    public void cancelInterview(int interviewId) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setStatus("CANCELLED");
        repository.save(interview);
    }

    @Override
    public List<Interview> getByApplication(int applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    @Override
    public List<Interview> getByStatus(String status) {
        return repository.findByStatus(status);
    }
}
