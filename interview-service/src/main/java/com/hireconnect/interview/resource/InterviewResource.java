package com.hireconnect.interview.resource;

import com.hireconnect.interview.dto.ApiResponse;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.service.InterviewService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/interviews")
public class InterviewResource {

    @Autowired
    private InterviewService service;

    @PostMapping
    public ResponseEntity<ApiResponse> schedule(@RequestBody Interview interview,
                                                HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"RECRUITER".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.of("Access denied: only Recruiter can schedule interviews", null));
        }
        return ResponseEntity.ok(ApiResponse.of("Interview scheduled successfully", service.scheduleInterview(interview)));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse> confirm(@PathVariable Long id,   // was int
                                               HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"CANDIDATE".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.of("Access denied: only Candidate can confirm interview", null));
        }
        return ResponseEntity.ok(ApiResponse.of("Interview confirmed successfully", service.confirmInterview(id)));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse> reschedule(@PathVariable Long id,    // was int
                                                  @RequestParam(required = false) String time,
                                                  @RequestBody(required = false) Map<String, Object> body,
                                                  HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"CANDIDATE".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.of("Access denied: only Candidate can reschedule", null));
        }
        String rawTime = time;
        if ((rawTime == null || rawTime.isBlank()) && body != null) {
            Object scheduledAt = body.get("scheduledAt");
            if (scheduledAt == null) {
                scheduledAt = body.get("time");
            }
            if (scheduledAt == null) {
                scheduledAt = body.get("newTime");
            }
            rawTime = scheduledAt == null ? null : scheduledAt.toString();
        }

        if (rawTime == null || rawTime.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of("Missing scheduled time", null));
        }

        LocalDateTime newTime = LocalDateTime.parse(rawTime);
        return ResponseEntity.ok(ApiResponse.of("Interview rescheduled successfully", service.rescheduleInterview(id, newTime)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancel(@PathVariable Long id,    // was int
                                              HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"RECRUITER".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.of("Access denied: only Recruiter can cancel", null));
        }
        service.cancelInterview(id);
        return ResponseEntity.ok(ApiResponse.of("Interview cancelled successfully", null));
    }

    @GetMapping("/application/{appId}")
    public ResponseEntity<ApiResponse> getByApplication(@PathVariable Long appId) {    // was int
        return ResponseEntity.ok(ApiResponse.of("Interviews fetched successfully", service.getByApplication(appId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.of("Interviews fetched successfully", service.getByStatus(status)));
    }
}
