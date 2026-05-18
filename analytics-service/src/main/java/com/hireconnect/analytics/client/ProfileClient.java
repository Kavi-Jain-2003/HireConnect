package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "profile-service")
public interface ProfileClient {

    @GetMapping("/profiles/public/recruiter/id/{id}")
    ApiResponse getRecruiterById(@PathVariable("id") Long id,
                                 @RequestHeader(value = "Authorization", required = false) String authHeader);

    default ApiResponse getRecruiterById(Long id) {
        return getRecruiterById(id, null);
    }
}
