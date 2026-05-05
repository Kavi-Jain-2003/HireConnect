package com.hireconnect.application.client;

import com.hireconnect.application.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service")
public interface JobClient {

    @GetMapping("/jobs/public/{id}")
    ApiResponse getJobById(@PathVariable("id") Long id);
}
