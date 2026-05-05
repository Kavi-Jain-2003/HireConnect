package com.hireconnect.job.client;

import com.hireconnect.job.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/public/users/email/{email}")
    ApiResponse getUserByEmail(@PathVariable("email") String email);
}
