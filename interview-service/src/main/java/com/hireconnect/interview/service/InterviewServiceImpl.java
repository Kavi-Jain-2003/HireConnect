package com.hireconnect.interview.service;

import com.hireconnect.interview.client.ApplicationClient;
import com.hireconnect.interview.client.AuthClient;
import com.hireconnect.interview.client.JobClient;
import com.hireconnect.interview.client.NotificationClient;
import com.hireconnect.interview.client.ProfileClient;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.messaging.NotificationEventPublisher;
import com.hireconnect.interview.dto.ApiResponse;
import com.hireconnect.interview.repository.InterviewRepository;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository repository;
    private final ApplicationClient applicationClient;
    private final ProfileClient profileClient;
    private final JobClient jobClient;
    private final AuthClient authClient;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper;
    private final NotificationEventPublisher eventPublisher; // Bug 1 fix: no longer null

    // Bug 1 fix: inject NotificationEventPublisher via constructor instead of hardcoding null
    public InterviewServiceImpl(
            InterviewRepository repository,
            ApplicationClient applicationClient,
            ProfileClient profileClient,
            JobClient jobClient,
            AuthClient authClient,
            NotificationClient notificationClient,
            ObjectMapper objectMapper,
            NotificationEventPublisher eventPublisher) {
        this.repository = repository;
        this.applicationClient = applicationClient;
        this.profileClient = profileClient;
        this.jobClient = jobClient;
        this.authClient = authClient;
        this.notificationClient = notificationClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;  // Bug 1 fix: properly injected
    }

    private Map<String, Object> getApplication(Long applicationId) {
        ApiResponse response = applicationClient.getApplicationById(applicationId);
        return toMap(response == null ? null : response.getData());
    }

    private Map<String, Object> getCandidate(Long candidateId) {
        ApiResponse response = profileClient.getCandidateById(candidateId);
        return toMap(response == null ? null : response.getData());
    }

    private Map<String, Object> getJob(Long jobId) {
        ApiResponse response = jobClient.getJobById(jobId);
        return toMap(response == null ? null : response.getData());
    }

    private Long resolveAuthUserId(String email) {
        try {
            ApiResponse response = authClient.getUserByEmail(email);
            Map<String, Object> user = toMap(response == null ? null : response.getData());
            if (user == null || user.get("userId") == null) return null;
            return Long.valueOf(user.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyCandidate(Interview interview, String eventLabel) {
        try {
            Map<String, Object> application = getApplication(interview.getApplicationId());
            if (application == null) return;

            Long candidateId = application.get("candidateId") == null
                    ? null : Long.valueOf(application.get("candidateId").toString());
            Long jobId = application.get("jobId") == null
                    ? null : Long.valueOf(application.get("jobId").toString());

            Map<String, Object> candidate = candidateId == null ? null : getCandidate(candidateId);
            Map<String, Object> job       = jobId == null       ? null : getJob(jobId);

            String candidateEmail = candidate == null || candidate.get("email") == null
                    ? null : candidate.get("email").toString();
            String jobTitle = job == null || job.get("title") == null
                    ? "your application" : job.get("title").toString();

            Long recipientUserId = candidateEmail == null ? null : resolveAuthUserId(candidateEmail);
            if (recipientUserId == null) return;

            // Bug 2 fix: declare the variables that were previously undefined
            String interviewDetails = interview.getMode() + " on " + interview.getScheduledAt()
                    + (interview.getMeetLink() != null ? " | Link: " + interview.getMeetLink() : "")
                    + (interview.getLocation()  != null ? " | Location: " + interview.getLocation() : "");

            // Bug 1 fix: eventPublisher is now properly injected, not null
            eventPublisher.publishInterviewScheduled(recipientUserId, interviewDetails, candidateEmail);

        } catch (Exception ignored) {
            // Interview scheduling should succeed even when notification delivery is temporarily unavailable.
        }
    }

    @Override
    public Interview scheduleInterview(Interview interview) {
        Interview existing = interview.getApplicationId() == null
                ? null
                : repository.findFirstByApplicationIdOrderByInterviewIdDesc(interview.getApplicationId())
                             .orElse(null);

        Interview saved;
        if (existing == null) {
            interview.setStatus("SCHEDULED");
            saved = repository.save(interview);
            notifyCandidate(saved, "scheduled");
            return saved;
        }

        boolean scheduleChanged =
                !java.util.Objects.equals(existing.getScheduledAt(), interview.getScheduledAt())
             || !java.util.Objects.equals(existing.getMode(),        interview.getMode())
             || !java.util.Objects.equals(existing.getMeetLink(),    interview.getMeetLink())
             || !java.util.Objects.equals(existing.getLocation(),    interview.getLocation())
             || !java.util.Objects.equals(existing.getNotes(),       interview.getNotes());

        existing.setApplicationId(interview.getApplicationId());
        existing.setScheduledAt(interview.getScheduledAt());
        existing.setMode(interview.getMode());
        existing.setMeetLink(interview.getMeetLink());
        existing.setLocation(interview.getLocation());
        existing.setNotes(interview.getNotes());
        existing.setStatus("SCHEDULED");
        saved = repository.save(existing);

        if (scheduleChanged) {
            notifyCandidate(saved, "rescheduled");
        }
        return saved;
    }

    @Override
    public Interview confirmInterview(Long interviewId) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setStatus("CONFIRMED");
        Interview saved = repository.save(interview);
        notifyCandidate(saved, "confirmed");
        return saved;
    }

    @Override
    public Interview rescheduleInterview(Long interviewId, LocalDateTime newTime) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setScheduledAt(newTime);
        interview.setStatus("RESCHEDULED");
        Interview saved = repository.save(interview);
        notifyCandidate(saved, "rescheduled");
        return saved;
    }

    @Override
    public void cancelInterview(Long interviewId) {
        Interview interview = repository.findById(interviewId).orElseThrow();
        interview.setStatus("CANCELLED");
        repository.save(interview);
        notifyCandidate(interview, "cancelled");
    }

    @Override
    public List<Interview> getByApplication(Long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    @Override
    public List<Interview> getByStatus(String status) {
        return repository.findByStatus(status);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object data) {
        if (data == null) return null;
        return objectMapper.convertValue(data, Map.class);
    }
}
