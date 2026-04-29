package com.hireconnect.analytics.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.analytics.client.ProfileClient;
import com.hireconnect.analytics.dto.AnalyticsSummary;
import com.hireconnect.analytics.dto.RecruiterProfileDTO;
import com.hireconnect.analytics.security.JwtUtil;
import com.hireconnect.analytics.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/analytics")
public class AnalyticsResource {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ProfileClient profileClient;

    @Autowired
    private ObjectMapper objectMapper;

    // Recruiter Analytics
    @GetMapping("/recruiter/{id}")
    public ResponseEntity<?> getRecruiterStats(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        RecruiterProfileDTO recruiter = getRecruiterProfile(id);
        if (recruiter == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Recruiter not found"));
        }

        if (!isAuthorized(authHeader, recruiter.getEmail(), false)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }

        return ResponseEntity.ok(analyticsService.getPipelineStats(id, authHeader));
    }

    // Admin Analytics
    @GetMapping("/admin")
    public ResponseEntity<?> getPlatformStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!isAuthorized(authHeader, null, true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required"));
        }

        return ResponseEntity.ok(analyticsService.getPlatformStats(authHeader));
    }

    // Per Job Metrics
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getJobStats(@PathVariable Long jobId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Missing token"));
        }

        Map<String, Object> data = new HashMap<>();

        data.put("views", analyticsService.getJobViewCount(jobId, authHeader));
        data.put("applications", analyticsService.getAppCountByJob(jobId, authHeader));
        data.put("ratio", analyticsService.getViewToApplyRatio(jobId, authHeader));
        data.put("timeToHire", analyticsService.getTimeToHire(jobId, authHeader));

        return ResponseEntity.ok(data);
    }

    private boolean isAuthorized(String authHeader, String recruiterEmail, boolean adminOnly) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            if (adminOnly) {
                return "ADMIN".equalsIgnoreCase(role);
            }

            if ("ADMIN".equalsIgnoreCase(role)) {
                return true;
            }

            if (!"RECRUITER".equalsIgnoreCase(role)) {
                return false;
            }

            return recruiterEmail != null && recruiterEmail.equalsIgnoreCase(email);
        } catch (Exception ex) {
            return false;
        }
    }

    private RecruiterProfileDTO getRecruiterProfile(Long id) {
        try {
            var response = profileClient.getRecruiterById(id);
            return objectMapper.convertValue(response == null ? null : response.getData(), RecruiterProfileDTO.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
