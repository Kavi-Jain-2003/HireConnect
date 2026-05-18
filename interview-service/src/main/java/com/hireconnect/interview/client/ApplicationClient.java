package com.hireconnect.interview.client;

import com.hireconnect.interview.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "application-service")
public interface ApplicationClient {

    @GetMapping("/applications/public/{id}")
    ApiResponse getApplicationById(@PathVariable("id") Long id);
}
