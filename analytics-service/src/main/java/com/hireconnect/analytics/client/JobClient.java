package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class JobClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.job-url}")
    private String jobServiceUrl;

    public ApiResponse getAllJobs() {
        return restTemplate.getForObject(jobServiceUrl + "/jobs/public", ApiResponse.class);
    }

    public ApiResponse getJobById(Long id) {
        if (id == null) {
            return null;
        }

        return restTemplate.getForObject(jobServiceUrl + "/jobs/public/" + id, ApiResponse.class);
    }

    public ApiResponse getAllJobs(String authHeader) {
        return exchangeGet(jobServiceUrl + "/jobs/public", authHeader);
    }

    private ApiResponse exchangeGet(String url, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class).getBody();
    }

    public ApiResponse getJobById(Long id, String authHeader) {
        if (id == null) {
            return null;
        }

        return exchangeGet(jobServiceUrl + "/jobs/public/" + id, authHeader);
    }
}
