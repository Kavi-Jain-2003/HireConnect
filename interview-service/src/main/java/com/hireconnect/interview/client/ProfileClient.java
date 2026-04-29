package com.hireconnect.interview.client;

import com.hireconnect.interview.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service")
public interface ProfileClient {

    @GetMapping("/profiles/public/candidate/id/{id}")
    ApiResponse getCandidateById(@PathVariable("id") Long id);
}
