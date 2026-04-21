package com.hireconnect.job.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.repository.JobRepository;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public String addJob(JobRequest request, String email) {

        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setCategory(request.getCategory());
        job.setType(request.getType());
        job.setLocation(request.getLocation());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSkills(request.getSkills());
        job.setExperienceRequired(request.getExperienceRequired());
        job.setDescription(request.getDescription());
        job.setCompany(request.getCompany());

        job.setPostedBy(email);
        job.setStatus("OPEN");
        job.setPostedAt(LocalDateTime.now());

        jobRepository.save(job);

        return "Job created successfully";
    }

    @Override
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @Override
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    @Override
    public List<Job> searchJobs(String title, String location) {

        if (title != null) {
            return jobRepository.findByTitleContainingIgnoreCase(title);
        }

        if (location != null) {
            return jobRepository.findByLocation(location);
        }

        return jobRepository.findAll();
    }

    @Override
    public String updateJob(Long id, JobRequest request, String email) {

        Job job = getJobById(id);

        if (!job.getPostedBy().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        job.setTitle(request.getTitle());
        job.setLocation(request.getLocation());
        job.setDescription(request.getDescription());

        jobRepository.save(job);

        return "Job updated";
    }

    @Override
    public String deleteJob(Long id, String email) {

        Job job = getJobById(id);

        if (!job.getPostedBy().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        jobRepository.delete(job);

        return "Job deleted";
    }
}
