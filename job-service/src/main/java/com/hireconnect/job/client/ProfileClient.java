package com.hireconnect.job.client;

import com.hireconnect.job.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service")
public interface ProfileClient {

    @GetMapping("/profiles/public/candidates")
    ApiResponse getAllCandidates();

    @GetMapping("/profiles/public/recruiters")
    ApiResponse getAllRecruiters();

    @GetMapping("/profiles/public/recruiter/id/{id}")
    ApiResponse getRecruiterById(@PathVariable("id") Long id);
}
