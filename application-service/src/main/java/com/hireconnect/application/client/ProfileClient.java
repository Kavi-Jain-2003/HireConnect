package com.hireconnect.application.client;

import com.hireconnect.application.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "profile-service")
public interface ProfileClient {

    @GetMapping("/profiles/public/candidate/email")
    ApiResponse getCandidateByEmail(@RequestParam("email") String email);

    @GetMapping("/profiles/public/candidate/id/{id}")
    ApiResponse getCandidateById(@PathVariable("id") Long id);
}
