package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ApplicationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.application-url}")
    private String applicationServiceUrl;

    public ApiResponse getApplicationsByJob(Long jobId) {
        if (jobId == null) {
            return null;
        }

        return restTemplate.getForObject(
                applicationServiceUrl + "/applications/job/" + jobId,
                ApiResponse.class);
    }

    public ApiResponse getApplicationsByJob(Long jobId, String authHeader) {
        if (jobId == null) {
            return null;
        }

        return exchangeGet(applicationServiceUrl + "/applications/job/" + jobId, authHeader);
    }

    public ApiResponse getAllApplications() {
        return restTemplate.getForObject(applicationServiceUrl + "/applications", ApiResponse.class);
    }

    public ApiResponse getAllApplications(String authHeader) {
        return exchangeGet(applicationServiceUrl + "/applications", authHeader);
    }

    public ApiResponse getApplicationById(Long id) {
        if (id == null) {
            return null;
        }

        return restTemplate.getForObject(
                applicationServiceUrl + "/applications/public/" + id,
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
