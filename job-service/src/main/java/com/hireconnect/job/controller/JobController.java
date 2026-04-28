package com.hireconnect.job.controller;

import java.util.List;

import com.hireconnect.job.dto.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobWithRecruiterDTO;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.service.JobService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createJob(@RequestBody JobRequest request,
                            HttpServletRequest httpRequest) {

        String email = (String) httpRequest.getAttribute("email");
        return ResponseEntity.ok(ApiResponse.of(jobService.addJob(request, email), null));
    }


    @GetMapping("/public")
    public ResponseEntity<ApiResponse> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.of("Jobs fetched successfully", jobService.getAllJobs()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse> getJob(@PathVariable Long id) {
    	//GET http://localhost:8082/jobs/1

        return ResponseEntity.ok(ApiResponse.of("Job fetched successfully", jobService.getJobById(id)));
    }

    @GetMapping("/public/search")
    public ResponseEntity<ApiResponse> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) Integer experience) {
//GET http://localhost:8082/jobs/search?location=Delhi

        return ResponseEntity.ok(ApiResponse.of("Search results", jobService.searchJobs(title, location, category, minSalary, maxSalary, experience)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateJob(@PathVariable Long id,
                            @RequestBody JobRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return ResponseEntity.ok(ApiResponse.of(jobService.updateJob(id, request, email), null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteJob(@PathVariable Long id) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return ResponseEntity.ok(ApiResponse.of(jobService.deleteJob(id, email), null));
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<ApiResponse> pauseJob(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(ApiResponse.of(jobService.pauseJob(id, email), null));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse> closeJob(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(ApiResponse.of(jobService.closeJob(id, email), null));
    }

    @GetMapping("/public/status/{status}")
    public ResponseEntity<ApiResponse> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.of("Jobs fetched successfully", jobService.getJobsByStatus(status)));
    }

    @GetMapping("/public/jobs-with-recruiter")
    public ResponseEntity<ApiResponse> getJobsWithRecruiter() {
        return ResponseEntity.ok(ApiResponse.of("Jobs fetched successfully", jobService.getAllJobsWithRecruiter()));
    }

}
