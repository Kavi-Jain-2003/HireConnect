package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InterviewClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.interview-url}")
    private String interviewServiceUrl;

    public ApiResponse getByApplication(Long appId) {
        if (appId == null) {
            return null;
        }

        return restTemplate.getForObject(
                interviewServiceUrl + "/interviews/application/" + appId,
                ApiResponse.class);
    }

    public ApiResponse getByApplication(Long appId, String authHeader) {
        if (appId == null) {
            return null;
        }

        return exchangeGet(interviewServiceUrl + "/interviews/application/" + appId, authHeader);
    }

    public ApiResponse getByStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return restTemplate.getForObject(
                interviewServiceUrl + "/interviews/status/" + status,
                ApiResponse.class);
    }

    private ApiResponse exchangeGet(String url, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class).getBody();
    }
}
