package com.hireconnect.interview.client;

import com.hireconnect.interview.dto.ApiResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications/public/dispatch")
    ApiResponse dispatch(@RequestBody Map<String, Object> payload);
}
