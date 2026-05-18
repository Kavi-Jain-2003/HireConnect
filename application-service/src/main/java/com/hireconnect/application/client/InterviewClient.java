package com.hireconnect.application.client;

import com.hireconnect.application.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "interview-service")
public interface InterviewClient {

    @GetMapping("/interviews/application/{appId}")
    ApiResponse getByApplication(@PathVariable("appId") Long appId);
}
