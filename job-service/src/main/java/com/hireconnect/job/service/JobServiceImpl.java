package com.hireconnect.job.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.hireconnect.job.client.AuthClient;
import com.hireconnect.job.client.NotificationClient;
import com.hireconnect.job.client.ProfileClient;
import com.hireconnect.job.dto.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobWithRecruiterDTO;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.repository.JobRepository;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ProfileClient profileClient;
    private final AuthClient authClient;
    private final NotificationClient notificationClient;

    public JobServiceImpl(JobRepository jobRepository, ProfileClient profileClient, AuthClient authClient, NotificationClient notificationClient) {
        this.jobRepository = jobRepository;
        this.profileClient = profileClient;
        this.authClient = authClient;
        this.notificationClient = notificationClient;
    }

    private Long resolveAuthUserId(String email) {
        try {
            ApiResponse response = authClient.getUserByEmail(email);
            Map<String, Object> user = toMap(response == null ? null : response.getData());
            if (user == null || user.get("userId") == null) {
                return null;
            }
            return Long.valueOf(user.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyCandidates(Job job) {
        try {
            ApiResponse candidateResponse = profileClient.getAllCandidates();
            List<Map<String, Object>> candidates = toList(candidateResponse == null ? null : candidateResponse.getData());
            if (candidates == null || candidates.isEmpty()) {
                return;
            }

            for (Map<String, Object> candidate : candidates) {
                Object email = candidate.get("email");
                if (email == null) {
                    continue;
                }

                Long recipientUserId = resolveAuthUserId(email.toString());
                if (recipientUserId == null) {
                    continue;
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", recipientUserId);
                payload.put("type", "JOB_ALERT");
                payload.put("subject", "New job alert");
                payload.put("email", email.toString());
                payload.put("message", "New job posted: \"" + job.getTitle() + "\" in " + job.getLocation() + ".");

                try {
                    notificationClient.dispatch(payload);
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

        ApiResponse recruiterResponse = profileClient.getAllRecruiters();
        List<Map<String, Object>> recruiters = toList(recruiterResponse == null ? null : recruiterResponse.getData());
        if (recruiters == null || recruiters.isEmpty()) {
            return result;
        }

        for (Job job : jobs) {

            for (Map<String, Object> r : recruiters) {

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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toList(Object data) {
        if (data == null) {
            return List.of();
        }
        return (List<Map<String, Object>>) data;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object data) {
        if (data == null) {
            return null;
        }
        return (Map<String, Object>) data;
    }
    // -----------------------------------------------------------------------
// PATCH — add this method to JobServiceImpl.java
// Also add the method signature to JobService interface (see JobService.java)
// -----------------------------------------------------------------------

    @Override
    @Transactional
    public Job incrementViewCount(Long id) {
        Job job = getJobById(id);
        int current = job.getViewCount() == null ? 0 : job.getViewCount();
        job.setViewCount(current + 1);
        return jobRepository.save(job);
    }

    // Admin deletes any job — no ownership check needed
    @Override
    public void adminDeleteJob(Long id) {
        Job job = getJobById(id); // throws if not found
        jobRepository.delete(job);
    }
}
