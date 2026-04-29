package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "interview-service")
public interface InterviewClient {

    @GetMapping("/interviews/application/{appId}")
    ApiResponse getByApplication(@PathVariable("appId") Long appId,
                                 @RequestHeader(value = "Authorization", required = false) String authHeader);

    default ApiResponse getByApplication(Long appId) {
        return getByApplication(appId, null);
    }

    @GetMapping("/interviews/status/{status}")
    ApiResponse getByStatus(@PathVariable("status") String status);
}
