package com.hireconnect.interview.service;

import com.hireconnect.interview.client.ApplicationClient;
import com.hireconnect.interview.client.JobClient;
import com.hireconnect.interview.client.NotificationClient;
import com.hireconnect.interview.client.ProfileClient;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.dto.ApiResponse;
import com.hireconnect.interview.repository.InterviewRepository;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository repository;
    private final ApplicationClient applicationClient;
    private final ProfileClient profileClient;
    private final JobClient jobClient;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper;

    public InterviewServiceImpl(
            InterviewRepository repository,
            ApplicationClient applicationClient,
            ProfileClient profileClient,
            JobClient jobClient,
            NotificationClient notificationClient,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.applicationClient = applicationClient;
        this.profileClient = profileClient;
        this.jobClient = jobClient;
        this.notificationClient = notificationClient;
        this.objectMapper = objectMapper;
    }

    private Map<String, Object> getApplication(long applicationId) {
        ApiResponse response = applicationClient.getApplicationById(applicationId);
        return toMap(response == null ? null : response.getData());
    }

    private Map<String, Object> getCandidate(long candidateId) {
        ApiResponse response = profileClient.getCandidateById(candidateId);
        return toMap(response == null ? null : response.getData());
    }

    private Map<String, Object> getJob(long jobId) {
        ApiResponse response = jobClient.getJobById(jobId);
        return toMap(response == null ? null : response.getData());
    }

    private void notifyCandidate(Interview interview, String eventLabel) {
        try {
            Map<String, Object> application = getApplication(interview.getApplicationId());
            Long candidateId = application.get("candidateId") == null
                    ? null
                    : Long.valueOf(application.get("candidateId").toString());
            Long jobId = application.get("jobId") == null
                    ? null
                    : Long.valueOf(application.get("jobId").toString());

            Map<String, Object> candidate = candidateId == null ? null : getCandidate(candidateId);
            Map<String, Object> job = jobId == null ? null : getJob(jobId);

            String candidateEmail = candidate == null || candidate.get("email") == null
                    ? null
                    : candidate.get("email").toString();
            String jobTitle = job == null || job.get("title") == null
                    ? "your application"
                    : job.get("title").toString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", candidateId);
            payload.put("type", "INTERVIEW");
            payload.put("subject", "Interview " + eventLabel.toLowerCase());
            payload.put("email", candidateEmail);
            payload.put("message", "Interview " + eventLabel.toLowerCase() + " for \"" + jobTitle
                    + "\". Scheduled for " + interview.getScheduledAt() + ".");

            notificationClient.dispatch(payload);
        } catch (Exception ignored) {
            // Interview scheduling should succeed even when notification delivery is temporarily unavailable.
        }
    }

    @Override
    public Interview scheduleInterview(Interview interview) {
        interview.setStatus("SCHEDULED");
        Interview saved = repository.save(interview);
        notifyCandidate(saved, "scheduled");
        return saved;
    }

    @Override
    public Interview confirmInterview(int interviewId) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setStatus("CONFIRMED");
        Interview saved = repository.save(interview);
        notifyCandidate(saved, "confirmed");
        return saved;
    }

    @Override
    public Interview rescheduleInterview(int interviewId, LocalDateTime newTime) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setScheduledAt(newTime);
        interview.setStatus("RESCHEDULED");
        Interview saved = repository.save(interview);
        notifyCandidate(saved, "rescheduled");
        return saved;
    }

    @Override
    public void cancelInterview(int interviewId) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setStatus("CANCELLED");
        repository.save(interview);
        notifyCandidate(interview, "cancelled");
    }

    @Override
    public List<Interview> getByApplication(int applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    @Override
    public List<Interview> getByStatus(String status) {
        return repository.findByStatus(status);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object data) {
        if (data == null) {
            return null;
        }
        return objectMapper.convertValue(data, Map.class);
    }
}
