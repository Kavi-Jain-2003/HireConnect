package com.hireconnect.application.controller;

import java.util.List;

import com.hireconnect.application.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.UpdateStatusRequest;
import com.hireconnect.application.entity.Application;
import com.hireconnect.application.service.ApplicationService;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    // ---------------- APPLY ----------------
    @PostMapping
    public ResponseEntity<ApiResponse> apply(@RequestBody ApplicationRequest request) {
        ApplicationResponse response = service.submitApplication(request);
        return ResponseEntity.ok(ApiResponse.of(response.getMessage(), response.getApplicationId()));
    }

    // ---------------- BY CANDIDATE ----------------
    @GetMapping("/candidate/{id}")
    public ResponseEntity<ApiResponse> getByCandidate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Applications fetched successfully", service.getByCandidate(id)));
    }

    // ---------------- BY JOB ----------------
    @GetMapping("/job/{id}")
    public ResponseEntity<ApiResponse> getByJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Applications fetched successfully", service.getByJob(id)));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Application fetched successfully", service.getApplicationById(id)));
    }

    // ---------------- UPDATE STATUS ----------------
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        ApplicationResponse response = service.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.of(response.getMessage(), response.getApplicationId()));
    }

    @PutMapping("/{id}/final-status")
    public ResponseEntity<ApiResponse> finalizeStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        ApplicationResponse response = service.finalizeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.of(response.getMessage(), response.getApplicationId()));
    }

    // ---------------- WITHDRAW ----------------
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> withdraw(@PathVariable Long id) {
        ApplicationResponse response = service.withdrawApplication(id);
        return ResponseEntity.ok(ApiResponse.of(response.getMessage(), response.getApplicationId()));
    }
    @GetMapping("/job/{id}/count")
    public ResponseEntity<ApiResponse> countByJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Application count fetched successfully", service.countByJob(id)));
    }

}
