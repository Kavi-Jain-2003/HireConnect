package com.hireconnect.job.service;

import java.time.LocalDateTime;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobWithRecruiterDTO;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.repository.JobRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    private void notifyCandidates(Job job) {
        try {
            List<Map> candidates = restTemplate.getForObject("http://localhost:8083/profiles/public/candidates", List.class);
            if (candidates == null || candidates.isEmpty()) {
                return;
            }

            for (Map candidate : candidates) {
                Object candidateId = candidate.get("profileId");
                Object email = candidate.get("email");
                if (candidateId == null || email == null) {
                    continue;
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", Long.valueOf(candidateId.toString()));
                payload.put("type", "JOB_ALERT");
                payload.put("subject", "New job alert");
                payload.put("email", email.toString());
                payload.put("message", "New job posted: \"" + job.getTitle() + "\" in " + job.getLocation() + ".");

                try {
                    restTemplate.postForObject("http://localhost:8086/notifications/public/dispatch", payload, Map.class);
                } catch (Exception ignored) {
                    // Keep job creation lightweight and resilient if notifications are temporarily unavailable.
                }
            }
        } catch (Exception ignored) {
            // Keep job creation lightweight even if profile or notification services are unavailable.
        }
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

        Job saved = jobRepository.save(job);
        notifyCandidates(saved);

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
    public List<Job> searchJobs(String title, String location, String category, Double minSalary, Double maxSalary, Integer experience) {

        List<Job> jobs = jobRepository.findAll();

        return jobs.stream()
                .filter(job -> title == null || title.isBlank()
                        || (job.getTitle() != null && job.getTitle().toLowerCase().contains(title.toLowerCase())))
                .filter(job -> location == null || location.isBlank()
                        || (job.getLocation() != null && job.getLocation().equalsIgnoreCase(location)))
                .filter(job -> category == null || category.isBlank()
                        || (job.getCategory() != null && job.getCategory().equalsIgnoreCase(category)))
                .filter(job -> minSalary == null
                        || (job.getSalaryMax() != null && job.getSalaryMax() >= minSalary))
                .filter(job -> maxSalary == null
                        || (job.getSalaryMin() != null && job.getSalaryMin() <= maxSalary))
                .filter(job -> experience == null
                        || (job.getExperienceRequired() != null && job.getExperienceRequired() <= experience))
                .toList();
    }

    @Override
    public String updateJob(Long id, JobRequest request, String email) {

        Job job = getJobById(id);

        if (!job.getPostedBy().equals(email)) {
            throw new RuntimeException("Unauthorized,You are not allowed to modify this job");
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
            throw new RuntimeException("Unauthorized,You are not allowed to modify this job");
        }

        jobRepository.delete(job);

        return "Job deleted";
    }

    @Override
    public String pauseJob(Long id, String email) {

        Job job = getJobById(id);

        if (!job.getPostedBy().equals(email)) {
            throw new RuntimeException("Unauthorized,You are not allowed to modify this job");
        }

        job.setStatus("PAUSED");
        jobRepository.save(job);

        return "Job paused";
    }

    @Override
    public String closeJob(Long id, String email) {

        Job job = getJobById(id);

        if (!job.getPostedBy().equals(email)) {
            throw new RuntimeException("Unauthorized,You are not allowed to modify this job");
        }

        job.setStatus("CLOSED");
        jobRepository.save(job);

        return "Job closed";
    }

    @Override
    public List<Job> getJobsByStatus(String status) {
        return jobRepository.findByStatus(status);
    }
   

    @Override
    public List<JobWithRecruiterDTO> getAllJobsWithRecruiter() {

        List<Job> jobs = jobRepository.findAll();
        List<JobWithRecruiterDTO> result = new ArrayList<>();

        RestTemplate restTemplate = new RestTemplate();

        String url = "http://localhost:8083/profiles/public/recruiters";
        List<Map> recruiters = restTemplate.getForObject(url, List.class);

        for (Job job : jobs) {

            for (Map r : recruiters) {

                if (r.get("email").equals(job.getPostedBy())) {

                    JobWithRecruiterDTO dto = new JobWithRecruiterDTO();

                    dto.setId(job.getJobId());
                    dto.setTitle(job.getTitle());
                    dto.setLocation(job.getLocation());
                    dto.setDescription(job.getDescription());

                    dto.setRecruiterName((String) r.get("fullName"));
                    dto.setCompanyName((String) r.get("companyName"));

                    result.add(dto);
                    break;
                }
            }
        }

        return result;
    }
}
