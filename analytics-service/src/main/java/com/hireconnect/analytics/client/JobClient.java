package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "job-service")
public interface JobClient {

    @GetMapping("/jobs/public")
    ApiResponse getAllJobs(@RequestHeader(value = "Authorization", required = false) String authHeader);

    default ApiResponse getAllJobs() {
        return getAllJobs(null);
    }

    @GetMapping("/jobs/public/{id}")
    ApiResponse getJobById(@PathVariable("id") Long id,
                           @RequestHeader(value = "Authorization", required = false) String authHeader);

    default ApiResponse getJobById(Long id) {
        return getJobById(id, null);
    }
}
