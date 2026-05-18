package com.hireconnect.analytics.service;

import com.hireconnect.analytics.client.ApplicationClient;
import com.hireconnect.analytics.client.InterviewClient;
import com.hireconnect.analytics.client.JobClient;
import com.hireconnect.analytics.client.ProfileClient;
import com.hireconnect.analytics.dto.AnalyticsSummary;
import com.hireconnect.analytics.dto.ApiResponse;
import com.hireconnect.analytics.dto.ApplicationDTO;
import com.hireconnect.analytics.dto.InterviewDTO;
import com.hireconnect.analytics.dto.JobDTO;
import com.hireconnect.analytics.dto.RecruiterProfileDTO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final JobClient jobClient;
    private final ApplicationClient applicationClient;
    private final InterviewClient interviewClient;
    private final ProfileClient profileClient;
    private final ObjectMapper objectMapper;

    public AnalyticsServiceImpl(
            JobClient jobClient,
            ApplicationClient applicationClient,
            InterviewClient interviewClient,
            ProfileClient profileClient,
            ObjectMapper objectMapper) {
        this.jobClient = jobClient;
        this.applicationClient = applicationClient;
        this.interviewClient = interviewClient;
        this.profileClient = profileClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getJobViewCount(Long jobId, String authHeader) {
        JobDTO job = getJob(jobId, authHeader);
        return job == null || job.getViewCount() == null ? 0 : Math.max(job.getViewCount(), 0);
    }

    @Override
    public int getAppCountByJob(Long jobId, String authHeader) {
        return getApplicationsByJob(jobId, authHeader).size();
    }

    @Override
    public double getViewToApplyRatio(Long jobId, String authHeader) {
        int views = getJobViewCount(jobId, authHeader);
        int applications = getAppCountByJob(jobId, authHeader);

        return views == 0 ? 0.0 : (double) applications / views;
    }

    @Override
    public double getTimeToHire(Long jobId, String authHeader) {
        return averageTimeToHireDays(getApplicationsByJob(jobId, authHeader), authHeader);
    }

    @Override
    public AnalyticsSummary getPipelineStats(Long recruiterId, String authHeader) {
        RecruiterProfileDTO recruiter = getRecruiter(recruiterId, authHeader);
        String recruiterEmail = recruiter == null ? null : recruiter.getEmail();

        List<JobDTO> recruiterJobs = getAllJobs(authHeader).stream()
                .filter(job -> recruiterEmail != null
                        && job.getPostedBy() != null
                        && job.getPostedBy().equalsIgnoreCase(recruiterEmail))
                .toList();

        Set<Long> recruiterJobIds = recruiterJobs.stream()
                .map(JobDTO::getJobId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ApplicationDTO> recruiterApplications = new ArrayList<>();
        for (Long jobId : recruiterJobIds) {
            recruiterApplications.addAll(getApplicationsByJob(jobId, authHeader));
        }

        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalJobs(recruiterJobs.size());
        summary.setTotalApplications(recruiterApplications.size());
        summary.setShortlistedCount(countByStatus(recruiterApplications, "SHORTLISTED"));
        summary.setOfferedCount(countByStatus(recruiterApplications, "OFFERED"));
        summary.setRejectedCount(countByStatus(recruiterApplications, "REJECTED"));
        summary.setAvgTimeToHireDays(averageTimeToHireDays(recruiterApplications, authHeader));
        summary.setViewToApplyRatio(calculateViewToApplyRatio(recruiterJobs, recruiterApplications));

        return summary;
    }

    @Override
    public AnalyticsSummary getPlatformStats(String authHeader) {
        List<JobDTO> jobs = getAllJobs(authHeader);
        List<ApplicationDTO> applications = new ArrayList<>();

        for (JobDTO job : jobs) {
            if (job.getJobId() != null) {
                applications.addAll(getApplicationsByJob(job.getJobId(), authHeader));
            }
        }

        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalJobs(jobs.size());
        summary.setTotalApplications(applications.size());
        summary.setShortlistedCount(countByStatus(applications, "SHORTLISTED"));
        summary.setOfferedCount(countByStatus(applications, "OFFERED"));
        summary.setRejectedCount(countByStatus(applications, "REJECTED"));
        summary.setAvgTimeToHireDays(averageTimeToHireDays(applications, authHeader));
        summary.setViewToApplyRatio(calculateViewToApplyRatio(jobs, applications));

        return summary;
    }

    @Override
    public Map<String, Long> getTopJobCategories(String authHeader) {
        return getAllJobs(authHeader).stream()
                .filter(job -> job.getCategory() != null && !job.getCategory().isBlank())
                .collect(Collectors.groupingBy(
                        JobDTO::getCategory,
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private int countByStatus(List<ApplicationDTO> applications, String status) {
        return (int) applications.stream()
                .filter(application -> application.getStatus() != null
                        && application.getStatus().equalsIgnoreCase(status))
                .count();
    }

    private double calculateViewToApplyRatio(List<JobDTO> jobs, List<ApplicationDTO> applications) {
        long totalViews = jobs.stream()
                .map(JobDTO::getViewCount)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        if (totalViews == 0L) {
            return 0.0;
        }

        return (double) applications.size() / totalViews;
    }

    private double averageTimeToHireDays(List<ApplicationDTO> applications, String authHeader) {
        List<Double> durations = new ArrayList<>();

        for (ApplicationDTO application : applications) {
            if (application.getApplicationId() == null || application.getAppliedAt() == null) {
                continue;
            }

            List<InterviewDTO> interviews = getInterviewsByApplication(application.getApplicationId(), authHeader);
            LocalDateTime firstScheduledAt = interviews.stream()
                    .map(InterviewDTO::getScheduledAt)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            if (firstScheduledAt == null) {
                continue;
            }

            Duration timeToHire = Duration.between(application.getAppliedAt(), firstScheduledAt);
            if (!timeToHire.isNegative()) {
                durations.add(timeToHire.toMinutes() / 1440.0);
            }
        }

        return durations.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private JobDTO getJob(Long jobId, String authHeader) {
        if (jobId == null) {
            return null;
        }

        ApiResponse response = jobClient.getJobById(jobId, authHeader);
        return toObject(response == null ? null : response.getData(), JobDTO.class);
    }

    private RecruiterProfileDTO getRecruiter(Long recruiterId, String authHeader) {
        if (recruiterId == null) {
            return null;
        }

        ApiResponse response = profileClient.getRecruiterById(recruiterId, authHeader);
        return toObject(response == null ? null : response.getData(), RecruiterProfileDTO.class);
    }

    private List<JobDTO> getAllJobs(String authHeader) {
        ApiResponse response = jobClient.getAllJobs(authHeader);
        return toList(response == null ? null : response.getData(), JobDTO.class);
    }

    private List<ApplicationDTO> getApplicationsByJob(Long jobId, String authHeader) {
        if (jobId == null) {
            return List.of();
        }

        ApiResponse response = applicationClient.getApplicationsByJob(jobId, authHeader);
        return toList(response == null ? null : response.getData(), ApplicationDTO.class);
    }

    private List<InterviewDTO> getInterviewsByApplication(Long applicationId, String authHeader) {
        if (applicationId == null) {
            return List.of();
        }

        ApiResponse response = interviewClient.getByApplication(applicationId, authHeader);
        return toList(response == null ? null : response.getData(), InterviewDTO.class);
    }

    private <T> List<T> toList(Object value, Class<T> elementType) {
        if (value == null) {
            return List.of();
        }

        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, elementType);
        return objectMapper.convertValue(value, listType);
    }

    private <T> T toObject(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        return objectMapper.convertValue(value, targetType);
    }
}
