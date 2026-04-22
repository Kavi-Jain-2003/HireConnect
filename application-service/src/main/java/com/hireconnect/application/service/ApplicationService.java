package com.hireconnect.application.service;

import java.util.List;

import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.UpdateStatusRequest;
import com.hireconnect.application.entity.Application;

public interface ApplicationService {

    ApplicationResponse submitApplication(ApplicationRequest request);

    List<Application> getByCandidate(Long candidateId);

    List<Application> getByJob(Long jobId);

    ApplicationResponse updateStatus(Long applicationId, UpdateStatusRequest request);

    ApplicationResponse withdrawApplication(Long applicationId);
}
