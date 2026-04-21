package com.hireconnect.job.controller;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
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
    public String createJob(@RequestBody JobRequest request,
                            HttpServletRequest httpRequest) {

        String email = (String) httpRequest.getAttribute("email");

        return jobService.addJob(request, email);
    }


    @GetMapping("/public")
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/public/{id}")
    public Job getJob(@PathVariable Long id) {
    	//GET http://localhost:8082/jobs/1

        return jobService.getJobById(id);
    }

    @GetMapping("/public/search")
    public List<Job> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location) {
//GET http://localhost:8082/jobs/search?location=Delhi

        return jobService.searchJobs(title, location);
    }

    @PutMapping("/{id}")
    public String updateJob(@PathVariable Long id,
                            @RequestBody JobRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return jobService.updateJob(id, request, email);
    }

    @DeleteMapping("/{id}")
    public String deleteJob(@PathVariable Long id) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return jobService.deleteJob(id, email);
    }
    @GetMapping("/public/jobs-with-recruiter")
    public List<JobWithRecruiterDTO> getJobsWithRecruiter() {
        return jobService.getAllJobsWithRecruiter();
    }

}
