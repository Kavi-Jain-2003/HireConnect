package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.repository.InterviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class InterviewServiceImpl implements InterviewService {

    @Autowired
    private InterviewRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, Object> getApplication(long applicationId) {
        return restTemplate.getForObject(
                "http://localhost:8084/applications/public/" + applicationId,
                Map.class
        );
    }

    private Map<String, Object> getCandidate(long candidateId) {
        return restTemplate.getForObject(
                "http://localhost:8083/profiles/public/candidate/id/" + candidateId,
                Map.class
        );
    }

    private Map<String, Object> getJob(long jobId) {
        return restTemplate.getForObject(
                "http://localhost:8082/jobs/public/" + jobId,
                Map.class
        );
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

            restTemplate.postForObject("http://localhost:8086/notifications/public/dispatch", payload, Map.class);
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
}
