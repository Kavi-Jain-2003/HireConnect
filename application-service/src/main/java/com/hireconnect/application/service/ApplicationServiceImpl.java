package com.hireconnect.application.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.UpdateStatusRequest;
import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.exception.ResourceNotFoundException;
import com.hireconnect.application.repository.ApplicationRepository;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.HashMap;
import com.hireconnect.application.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class ApplicationServiceImpl implements ApplicationService {

	private final ApplicationRepository repository;
	private final RestTemplate restTemplate;

	public ApplicationServiceImpl(ApplicationRepository repository, RestTemplate restTemplate) {
		this.repository = repository;
		this.restTemplate = restTemplate;
	}

    private String getLoggedInUserEmail() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getLoggedInUserRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
    }

    private Map<String, Object> validateJob(Long jobId) {
        try {
            return restTemplate.getForObject("http://localhost:8082/jobs/public/" + jobId, Map.class);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Job not found with id: " + jobId);
        }
    }

    private Long validateCandidate(String email) {
        try {
            Map candidate = restTemplate.getForObject(
                    "http://localhost:8083/profiles/public/candidate/email/" + email,
                    Map.class
            );

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
            return restTemplate.getForObject(
                    "http://localhost:8083/profiles/public/candidate/id/" + candidateId,
                    Map.class
            );
        } catch (Exception e) {
            throw new ResourceNotFoundException("Candidate not found with id: " + candidateId);
        }
    }

    private void notifyCandidate(Long candidateId, Long jobId, String status) {
        try {
            Map<String, Object> job = validateJob(jobId);
            Map<String, Object> candidate = getCandidateById(candidateId);

            String candidateEmail = candidate.get("email") == null ? null : candidate.get("email").toString();
            String jobTitle = job.get("title") == null ? "your job application" : job.get("title").toString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", candidateId);
            payload.put("type", "APPLICATION_STATUS");
            payload.put("subject", "Application update");
            payload.put("email", candidateEmail);
            payload.put("message", "Your application for \"" + jobTitle + "\" is now " + status.replace('_', ' ') + ".");

            restTemplate.postForObject("http://localhost:8086/notifications/public/dispatch", payload, Map.class);
        } catch (Exception ignored) {
            // Keep application updates lightweight and resilient if notifications are temporarily unavailable.
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

		return new ApplicationResponse("Application submitted successfully", saved.getApplicationId());
	}
//Applied → Shortlisted → Interview Scheduled →	Offered / Rejected) 
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
		notifyCandidate(app.getCandidateId(), app.getJobId(), next.name().replace('_', ' '));

		return new ApplicationResponse("Application status updated successfully", app.getApplicationId());
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

}
