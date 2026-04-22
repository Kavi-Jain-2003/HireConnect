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

    private void validateJob(Long jobId) {
        try {
            restTemplate.getForObject("http://localhost:8082/jobs/public/" + jobId, Object.class);
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

	// APPLY (ONLY CANDIDATE)
	@Override
    public ApplicationResponse submitApplication(ApplicationRequest request) {

        if (!getLoggedInUserRole().equals("ROLE_CANDIDATE")) {
            throw new RuntimeException("Only candidates can apply");
        }

        String email = getLoggedInUserEmail();
        Long candidateId = validateCandidate(email);

        validateJob(request.getJobId());

        repository.findFirstByJobIdAndCandidateId(request.getJobId(), candidateId).ifPresent(a -> {
            throw new RuntimeException("Already applied for this job");
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

	// UPDATE STATUS (ONLY RECRUITER)
	@Override
	public ApplicationResponse updateStatus(Long applicationId, UpdateStatusRequest request) {

		if (!getLoggedInUserRole().equals("ROLE_RECRUITER")) {
			throw new RuntimeException("Only recruiters can update status");
		}

		Application app = repository.findById(applicationId)
				.orElseThrow(() -> new ResourceNotFoundException("Application not found"));

		app.setStatus(request.getStatus());
		repository.save(app);

		return new ApplicationResponse("Application status updated successfully", app.getApplicationId());
	}

	@Override
    public ApplicationResponse withdrawApplication(Long applicationId) {

        String email = getLoggedInUserEmail();
        Long candidateId = validateCandidate(email);

        Application app = repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

		if (!app.getCandidateId().equals(candidateId)) {
			throw new RuntimeException("You can only withdraw your own application");
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
}
