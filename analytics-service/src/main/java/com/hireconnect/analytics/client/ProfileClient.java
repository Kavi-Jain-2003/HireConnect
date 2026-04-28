package com.hireconnect.analytics.client;

import com.hireconnect.analytics.dto.ApiResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProfileClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.profile-url}")
    private String profileServiceUrl;

    public ApiResponse getRecruiterById(Long id) {
        if (id == null) {
            return null;
        }

        return restTemplate.getForObject(
                profileServiceUrl + "/profiles/public/recruiter/id/" + id,
                ApiResponse.class);
    }

    public ApiResponse getRecruiterById(Long id, String authHeader) {
        if (id == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
                profileServiceUrl + "/profiles/public/recruiter/id/" + id,
                HttpMethod.GET,
                entity,
                ApiResponse.class).getBody();
    }
}
