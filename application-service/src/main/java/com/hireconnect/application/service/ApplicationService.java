package com.hireconnect.application.service;

import java.util.List;

import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.UpdateStatusRequest;
import com.hireconnect.application.entity.Application;

public interface ApplicationService {

    ApplicationResponse submitApplication(ApplicationRequest request);

    // candidateId is the profile ID being requested; the impl verifies
    // the logged-in user owns that profile before returning data.
    List<Application> getByCandidate(Long candidateId);

    List<Application> getByJob(Long jobId);

    Application getApplicationById(Long applicationId);

    ApplicationResponse updateStatus(Long applicationId, UpdateStatusRequest request);

    ApplicationResponse finalizeStatus(Long applicationId, UpdateStatusRequest request);

    ApplicationResponse withdrawApplication(Long applicationId);

    Long countByJob(Long jobId);
}
