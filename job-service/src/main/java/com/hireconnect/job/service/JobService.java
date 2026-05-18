package com.hireconnect.job.service;

import java.util.List;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobWithRecruiterDTO;
import com.hireconnect.job.entity.Job;

public interface JobService {

    String addJob(JobRequest request, String recruiterEmail, Long recruiterUserId);

    List<Job> getAllJobs();

    Job getJobById(Long id);

    List<Job> searchJobs(String title, String location, String category,
                         Double minSalary, Double maxSalary, Integer experience);

    String updateJob(Long id, JobRequest request, String userEmail);

    String deleteJob(Long id, String userEmail);

    String pauseJob(Long id, String userEmail);

    String closeJob(Long id, String userEmail);

    List<Job> getJobsByStatus(String status);

    List<JobWithRecruiterDTO> getAllJobsWithRecruiter();

    // Fix 7: increments viewCount by 1 and returns updated job
    Job incrementViewCount(Long id);

    // Admin can delete any job without ownership check
    void adminDeleteJob(Long id);
}