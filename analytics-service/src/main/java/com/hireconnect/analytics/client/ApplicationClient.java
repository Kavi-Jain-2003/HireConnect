package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "application-service")
public interface ApplicationClient {

    @GetMapping("/applications/job/{jobId}")
    ApiResponse getApplicationsByJob(@PathVariable("jobId") Long jobId,
                                     @RequestHeader(value = "Authorization", required = false) String authHeader);

    default ApiResponse getApplicationsByJob(Long jobId) {
        return getApplicationsByJob(jobId, null);
    }

    @GetMapping("/applications")
    ApiResponse getAllApplications(@RequestHeader(value = "Authorization", required = false) String authHeader);
 
    default ApiResponse getAllApplications() {
        return getAllApplications(null);
    }

    @GetMapping("/applications/public/{id}")
    ApiResponse getApplicationById(@PathVariable("id") Long id);
}
