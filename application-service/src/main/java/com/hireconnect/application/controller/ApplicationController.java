package com.hireconnect.application.controller;

import java.util.List;

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
    public ResponseEntity<ApplicationResponse> apply(@RequestBody ApplicationRequest request) {
        return ResponseEntity.ok(service.submitApplication(request));
    }

    // ---------------- BY CANDIDATE ----------------
    @GetMapping("/candidate/{id}")
    public ResponseEntity<List<Application>> getByCandidate(@PathVariable Long id) {
        return ResponseEntity.ok(service.getByCandidate(id));
    }

    // ---------------- BY JOB ----------------
    @GetMapping("/job/{id}")
    public ResponseEntity<List<Application>> getByJob(@PathVariable Long id) {
        return ResponseEntity.ok(service.getByJob(id));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<Application> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getApplicationById(id));
    }

    // ---------------- UPDATE STATUS ----------------
    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    // ---------------- WITHDRAW ----------------
    @DeleteMapping("/{id}")
    public ResponseEntity<ApplicationResponse> withdraw(@PathVariable Long id) {
        return ResponseEntity.ok(service.withdrawApplication(id));
    }
    @GetMapping("/job/{id}/count")
    public ResponseEntity<Long> countByJob(@PathVariable Long id) {
        return ResponseEntity.ok(service.countByJob(id));
    }

}
