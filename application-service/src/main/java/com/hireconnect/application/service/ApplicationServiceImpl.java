package com.hireconnect.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.UpdateStatusRequest;
import com.hireconnect.application.dto.ApiResponse;
import com.hireconnect.application.client.AuthClient;
import com.hireconnect.application.client.InterviewClient;
import com.hireconnect.application.client.JobClient;
import com.hireconnect.application.client.NotificationClient;
import com.hireconnect.application.client.ProfileClient;
import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.exception.ResourceNotFoundException;
import com.hireconnect.application.messaging.NotificationEventPublisher;
import com.hireconnect.application.repository.ApplicationRepository;
import java.util.Map;
import java.util.HashMap;
import com.hireconnect.application.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final ApplicationRepository repository;
    private final JobClient jobClient;
    private final ProfileClient profileClient;
    private final AuthClient authClient;
    private final InterviewClient interviewClient;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper;
    private final NotificationEventPublisher eventPublisher;

    public ApplicationServiceImpl(
            ApplicationRepository repository,
            JobClient jobClient,
            ProfileClient profileClient,
            AuthClient authClient,
            InterviewClient interviewClient,
            NotificationClient notificationClient,
            ObjectMapper objectMapper,
        NotificationEventPublisher eventPublisher) {
        this.repository = repository;
        this.jobClient = jobClient;
        this.profileClient = profileClient;
        this.authClient = authClient;
        this.interviewClient = interviewClient;
        this.notificationClient = notificationClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    private String getLoggedInUserEmail() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getLoggedInUserRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
    }

    private Map<String, Object> validateJob(Long jobId) {
        try {
            ApiResponse response = jobClient.getJobById(jobId);
            return toMap(response == null ? null : response.getData());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Job not found with id: " + jobId);
        }
    }

    private Long validateCandidate(String email) {
        try {
            ApiResponse response = profileClient.getCandidateByEmail(email);
            Map<String, Object> candidate = toMap(response == null ? null : response.getData());

            if (candidate == null || candidate.get("profileId") == null) {
                throw new ResourceNotFoundException("Candidate not found with email: " + email);
            }

            return Long.valueOf(candidate.get("profileId").toString());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Candidate not found with email: " + email);
        }
    }

    private Map<String, Object> getCandidateById(Long candidateId) {
        try {
            ApiResponse response = profileClient.getCandidateById(candidateId);
            return toMap(response == null ? null : response.getData());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Candidate not found with id: " + candidateId);
        }
    }

    private Long resolveAuthUserId(String email) {
        try {
            ApiResponse response = authClient.getUserByEmail(email);
            Map<String, Object> user = toMap(response == null ? null : response.getData());
            if (user == null || user.get("userId") == null) {
                return null;
            }
            return Long.valueOf(user.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveCandidateUserId(Map<String, Object> candidate, String email) {
        if (candidate != null && candidate.get("userId") != null) {
            try {
                return Long.valueOf(candidate.get("userId").toString());
            } catch (Exception ignored) {
                // Fall back to auth lookup below.
            }
        }
        return email == null ? null : resolveAuthUserId(email);
    }

    private boolean hasConfirmedInterview(Long applicationId) {
        try {
            ApiResponse response = interviewClient.getByApplication(applicationId);
            List<Map<String, Object>> interviews = toListOfMaps(response == null ? null : response.getData());
            if (interviews.isEmpty()) {
                return false;
            }

            Map<String, Object> latestInterview = interviews.stream()
                    .filter(item -> item != null && item.get("scheduledAt") != null)
                    .max((left, right) -> {
                        LocalDateTime leftTime = parseDateTime(left.get("scheduledAt"));
                        LocalDateTime rightTime = parseDateTime(right.get("scheduledAt"));
                        if (leftTime == null && rightTime == null) {
                            return 0;
                        }
                        if (leftTime == null) {
                            return -1;
                        }
                        if (rightTime == null) {
                            return 1;
                        }
                        return leftTime.compareTo(rightTime);
                    })
                    .orElse(null);

            if (latestInterview == null || latestInterview.get("status") == null) {
                return false;
            }

            return "CONFIRMED".equalsIgnoreCase(latestInterview.get("status").toString());
        } catch (Exception ex) {
            log.warn("Failed to verify interview confirmation for applicationId={}", applicationId, ex);
            return false;
        }
    }

    private void notifyCandidate(Long candidateId, Long jobId, String status) {
        try {
            Map<String, Object> job = validateJob(jobId);
            Map<String, Object> candidate = getCandidateById(candidateId);

            String candidateEmail = candidate.get("email") == null ? null : candidate.get("email").toString();
            String jobTitle = job.get("title") == null ? "your job application" : job.get("title").toString();
            Long recipientUserId = resolveCandidateUserId(candidate, candidateEmail);

            String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
            String message;
            String subject;

            switch (normalizedStatus) {
                case "APPLIED":
                case "SUBMITTED":
                    subject = "Application submitted";
                    message = "Your application for \"" + jobTitle + "\" has been submitted successfully.";
                    break;
                case "SHORTLISTED":
                    subject = "Application shortlisted";
                    message = "Your application for \"" + jobTitle + "\" has been shortlisted.";
                    break;
                case "INTERVIEW_SCHEDULED":
                    subject = "Interview scheduled";
                    message = "Your application for \"" + jobTitle + "\" is now interview scheduled.";
                    break;
                case "OFFERED":
                    subject = "Final offer received";
                    message = "You have received a final offer for \"" + jobTitle + "\".";
                    break;
                case "REJECTED":
                    subject = "Application update";
                    message = "Your application for \"" + jobTitle + "\" was not selected.";
                    break;
                default:
                    subject = "Application update";
                    message = "Your application for \"" + jobTitle + "\" is now "
                            + normalizedStatus.replace('_', ' ').toLowerCase(Locale.ROOT) + ".";
                    break;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", recipientUserId);
            payload.put("type", "APPLICATION_STATUS");
            payload.put("subject", subject);
            payload.put("email", candidateEmail);
            payload.put("message", message);

            notificationClient.dispatch(payload);
        } catch (Exception ex) {
            log.warn("Failed to dispatch application notification for candidateId={}, jobId={}, status={}",
                    candidateId, jobId, status, ex);
        }
    }
    private void notifyRecruiter(Long jobId, Long candidateId) {
    try {
        Map<String, Object> job = validateJob(jobId);
        String recruiterEmail = job.get("recruiterEmail") == null ? null : job.get("recruiterEmail").toString();
        String jobTitle = job.get("title") == null ? "a job" : job.get("title").toString();
        Long recruiterUserId = job.get("recruiterUserId") == null ? null : Long.valueOf(job.get("recruiterUserId").toString());

        String message = "A new candidate has applied for \"" + jobTitle + "\".";
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", recruiterUserId);
        payload.put("type", "APPLICATION_STATUS");
        payload.put("subject", "New application received");
        payload.put("email", recruiterEmail);
        payload.put("message", message);

        notificationClient.dispatch(payload);
    } catch (Exception ex) {
        log.warn("Failed to notify recruiter for jobId={}", jobId, ex);
    }
}

    // APPLY (ONLY CANDIDATE)
    @Override
    public ApplicationResponse submitApplication(ApplicationRequest request) {

        if (!getLoggedInUserRole().equals("ROLE_CANDIDATE")) {
            throw new BusinessException("Only candidates can apply");

        }

        String email = getLoggedInUserEmail();
        Long candidateId = validateCandidate(email);

        validateJob(request.getJobId());

        repository.findFirstByJobIdAndCandidateId(request.getJobId(), candidateId)
                .ifPresent(existing -> {

                    if (existing.getStatus() != ApplicationStatus.WITHDRAWN) {
                        throw new BusinessException("You have already applied for this job");
                    }

                    // ✅ If withdrawn → allow reapply by deleting old record OR updating it
                    repository.delete(existing);
                    // OR alternatively:
                    // existing.setStatus(ApplicationStatus.APPLIED);
                    // existing.setAppliedAt(LocalDateTime.now());
                    // repository.save(existing);
                });

        Application app = new Application();
        app.setJobId(request.getJobId());
        app.setCandidateId(candidateId);
        app.setCoverLetter(request.getCoverLetter());
        app.setResumeUrl(request.getResumeUrl());
        app.setAppliedAt(LocalDateTime.now());
        app.setStatus(ApplicationStatus.APPLIED);

        Application saved = repository.save(app);
        notifyCandidate(saved.getCandidateId(), saved.getJobId(), "APPLIED");

        return new ApplicationResponse("Application submitted successfully", saved.getApplicationId());
    }

    // Applied → Shortlisted → Interview Scheduled → Offered / Rejected)
    // UPDATE STATUS (ONLY RECRUITER)
    @Override
    public ApplicationResponse updateStatus(Long applicationId, UpdateStatusRequest request) {

        if (!getLoggedInUserRole().equals("ROLE_RECRUITER")) {
            throw new BusinessException("Only recruiters can update status");
        }

        Application app = repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        ApplicationStatus current = app.getStatus();
        ApplicationStatus next = request.getStatus();

        // ❗ Prevent change after final state
        if (current == ApplicationStatus.OFFERED || current == ApplicationStatus.REJECTED) {
            throw new BusinessException("Application already finalized");
        }

        // ❗ Enforce valid transitions
        if (current == ApplicationStatus.APPLIED && next != ApplicationStatus.SHORTLISTED) {
            throw new BusinessException("Invalid status transition: APPLIED → " + next);
        }

        if (current == ApplicationStatus.SHORTLISTED && next != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new BusinessException("Invalid status transition: SHORTLISTED → " + next);
        }

        if (current == ApplicationStatus.INTERVIEW_SCHEDULED &&
                !(next == ApplicationStatus.OFFERED || next == ApplicationStatus.REJECTED)) {
            throw new BusinessException("Invalid status transition: INTERVIEW_SCHEDULED → " + next);
        }

        app.setStatus(next);
        repository.save(app);
        notifyCandidate(app.getCandidateId(), app.getJobId(), next.name());

        return new ApplicationResponse("Application status updated successfully", app.getApplicationId());
    }

    @Override
    public ApplicationResponse finalizeStatus(Long applicationId, UpdateStatusRequest request) {

        if (!getLoggedInUserRole().equals("ROLE_RECRUITER")) {
            throw new BusinessException("Only recruiters can update status");
        }

        Application app = repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        ApplicationStatus current = app.getStatus();
        ApplicationStatus next = request.getStatus();

        if (next != ApplicationStatus.OFFERED && next != ApplicationStatus.REJECTED) {
            throw new BusinessException("Final status must be OFFERED or REJECTED");
        }

        if (next == ApplicationStatus.OFFERED && !hasConfirmedInterview(applicationId)) {
            throw new BusinessException("Candidate can only be offered after the interview is confirmed");
        }

        if (current == ApplicationStatus.OFFERED || current == ApplicationStatus.REJECTED) {
            throw new BusinessException("Application already finalized");
        }

        app.setStatus(next);
        repository.save(app);
        notifyCandidate(app.getCandidateId(), app.getJobId(), next.name());

        return new ApplicationResponse("Application finalized successfully", app.getApplicationId());
    }

    @Override
    public ApplicationResponse withdrawApplication(Long applicationId) {

        String email = getLoggedInUserEmail();
        Long candidateId = validateCandidate(email);

        Application app = repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!app.getCandidateId().equals(candidateId)) {
            throw new BusinessException("You can only withdraw your own application");
        }
        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BusinessException("Application already withdrawn");
        }

        // ❗ Prevent withdraw after final decision
        if (app.getStatus() == ApplicationStatus.OFFERED ||
                app.getStatus() == ApplicationStatus.REJECTED) {

            throw new BusinessException("Cannot withdraw after final decision");
        }

        app.setStatus(ApplicationStatus.WITHDRAWN);
        repository.save(app);

        return new ApplicationResponse("Application withdrawn successfully", app.getApplicationId());
    }

    @Override
    public List<Application> getByCandidate(Long candidateId) {

        // Ownership check: a candidate may only fetch their own applications.
        // Recruiters and Admins can fetch any candidate's applications.
        String role = getLoggedInUserRole();

        if ("ROLE_CANDIDATE".equals(role)) {
            String email = getLoggedInUserEmail();
            Long loggedInCandidateId = validateCandidate(email);

            if (!loggedInCandidateId.equals(candidateId)) {
                throw new BusinessException("Access denied: you can only view your own applications");
            }
        }
        // ROLE_RECRUITER and ROLE_ADMIN pass through without restriction.

        return repository.findByCandidateId(candidateId);
    }

    @Override
    public List<Application> getByJob(Long jobId) {
        return repository.findByJobId(jobId);
    }

    @Override
    public Application getApplicationById(Long applicationId) {
        return repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    @Override
    public Long countByJob(Long jobId) {

        // optional: validate job exists
        validateJob(jobId);

        return repository.countByJobId(jobId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object data) {
        if (data == null) {
            return null;
        }
        return objectMapper.convertValue(data, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toListOfMaps(Object data) {
        if (data == null) {
            return List.of();
        }
        return objectMapper.convertValue(data, List.class);
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

}
