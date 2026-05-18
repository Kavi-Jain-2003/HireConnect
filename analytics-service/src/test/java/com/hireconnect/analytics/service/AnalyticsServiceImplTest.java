package com.hireconnect.analytics.service;

// ════════════════════════════════════════════════════════════════
//  AnalyticsServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  getJobViewCount()       — views return hon ✅
//    2.  getJobViewCount()       — null job → 0 return ✅
//    3.  getAppCountByJob()      — application count ✅
//    4.  getViewToApplyRatio()   — ratio calculate ho ✅
//    5.  getViewToApplyRatio()   — 0 views → 0.0 return ✅
//    6.  getTopJobCategories()   — categories group ho ✅
//    7.  getTopJobCategories()   — empty list ✅
//    8.  getPlatformStats()      — total stats ✅
//    9.  getPlatformStats()      — counts by status ✅
//    10. getPipelineStats()      — recruiter specific stats ✅
// ════════════════════════════════════════════════════════════════

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.analytics.client.ApplicationClient;
import com.hireconnect.analytics.client.InterviewClient;
import com.hireconnect.analytics.client.JobClient;
import com.hireconnect.analytics.client.ProfileClient;
import com.hireconnect.analytics.dto.AnalyticsSummary;
import com.hireconnect.analytics.dto.ApiResponse;
import com.hireconnect.analytics.dto.ApplicationDTO;
import com.hireconnect.analytics.dto.JobDTO;
import com.hireconnect.analytics.dto.RecruiterProfileDTO;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock private JobClient jobClient;
    @Mock private ApplicationClient applicationClient;
    @Mock private InterviewClient interviewClient;
    @Mock private ProfileClient profileClient;

    // Real ObjectMapper use karte hain — DTO conversion ke liye
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ── Real class ────────────────────────────────────────────────
    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private static final String AUTH = "Bearer test.token";

    // ── Test Data ─────────────────────────────────────────────────
    private JobDTO job1, job2;
    private ApplicationDTO app1, app2, app3;
    private ApiResponse jobResponse, appsResponse, emptyResponse; 

    @BeforeEach
    void setUp() {
        // ObjectMapper inject karo manually
        analyticsService = new AnalyticsServiceImpl(
                jobClient, applicationClient, interviewClient, profileClient, objectMapper);

        // Sample Jobs
        job1 = new JobDTO();
        job1.setJobId(1L);
        job1.setTitle("Java Developer");
        job1.setCategory("IT");
        job1.setPostedBy("recruiter1@gmail.com");
        job1.setViewCount(100);

        job2 = new JobDTO();
        job2.setJobId(2L);
        job2.setTitle("CSS Developer");
        job2.setCategory("Design");
        job2.setPostedBy("recruiter1@gmail.com");
        job2.setViewCount(50);

        // Sample Applications
        app1 = new ApplicationDTO();
        app1.setApplicationId(1L);
        app1.setJobId(1L);
        app1.setStatus("SHORTLISTED");
        app1.setAppliedAt(LocalDateTime.now().minusDays(5));

        app2 = new ApplicationDTO();
        app2.setApplicationId(2L);
        app2.setJobId(1L);
        app2.setStatus("OFFERED");
        app2.setAppliedAt(LocalDateTime.now().minusDays(3));

        app3 = new ApplicationDTO();
        app3.setApplicationId(3L);
        app3.setJobId(1L);
        app3.setStatus("REJECTED");
        app3.setAppliedAt(LocalDateTime.now().minusDays(1));

        // API Responses
        jobResponse = new ApiResponse("success", job1);
        appsResponse = new ApiResponse("success", Arrays.asList(app1, app2, app3));
        emptyResponse = new ApiResponse("success", Collections.emptyList());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — getJobViewCount() — Views return hon ✅
    // Kya test: Job ka view count sahi aaye
    // Expected: 100 views
    // ════════════════════════════════════════════════════════════
    @Test
    void getJobViewCount_WhenJobExists_ShouldReturnViewCount() {
        // ARRANGE
        when(jobClient.getJobById(1L, AUTH)).thenReturn(jobResponse);

        // ACT
        int result = analyticsService.getJobViewCount(1L, AUTH);

        // ASSERT
        assertEquals(100, result);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — getJobViewCount() — Null Job → 0 ✅
    // Kya test: Job nahi mili toh 0 return ho
    // ════════════════════════════════════════════════════════════
    @Test
    void getJobViewCount_WhenJobNotFound_ShouldReturnZero() {
        // ARRANGE — null response
        when(jobClient.getJobById(999L, AUTH)).thenReturn(null);

        // ACT
        int result = analyticsService.getJobViewCount(999L, AUTH);

        // ASSERT
        assertEquals(0, result);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — getAppCountByJob() — Application count ✅
    // Kya test: Job ke liye kitni applications hain
    // Expected: 3 applications
    // ════════════════════════════════════════════════════════════
    @Test
    void getAppCountByJob_ShouldReturnCorrectCount() {
        // ARRANGE
        when(applicationClient.getApplicationsByJob(1L, AUTH)).thenReturn(appsResponse);

        // ACT
        int result = analyticsService.getAppCountByJob(1L, AUTH);

        // ASSERT
        assertEquals(3, result);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — getViewToApplyRatio() — Ratio calculate ho ✅
    // Kya test: 100 views, 3 applications → ratio = 0.03
    // ════════════════════════════════════════════════════════════
    @Test
    void getViewToApplyRatio_ShouldCalculateCorrectly() {
        // ARRANGE
        when(jobClient.getJobById(1L, AUTH)).thenReturn(jobResponse);
        when(applicationClient.getApplicationsByJob(1L, AUTH)).thenReturn(appsResponse);

        // ACT
        double ratio = analyticsService.getViewToApplyRatio(1L, AUTH);

        // ASSERT
        assertEquals(0.03, ratio, 0.001); // 3/100 = 0.03
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — getViewToApplyRatio() — 0 views → 0.0 ✅
    // Kya test: Division by zero handle ho
    // ════════════════════════════════════════════════════════════
    @Test
    void getViewToApplyRatio_WhenZeroViews_ShouldReturnZero() {
        // ARRANGE — 0 views
        job1.setViewCount(0);
        when(jobClient.getJobById(1L, AUTH)).thenReturn(jobResponse);
        when(applicationClient.getApplicationsByJob(1L, AUTH)).thenReturn(appsResponse);

        // ACT
        double ratio = analyticsService.getViewToApplyRatio(1L, AUTH);

        // ASSERT
        assertEquals(0.0, ratio);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — getTopJobCategories() — Categories group hon ✅
    // Kya test: IT aur Design categories count hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getTopJobCategories_ShouldGroupByCategory() {
        // ARRANGE — 2 IT jobs, 1 Design job
        JobDTO job3 = new JobDTO();
        job3.setJobId(3L);
        job3.setCategory("IT");
        job3.setPostedBy("recruiter2@gmail.com");

        ApiResponse allJobsResponse = new ApiResponse("success",
                Arrays.asList(job1, job2, job3));

        when(jobClient.getAllJobs(AUTH)).thenReturn(allJobsResponse);

        // ACT
        Map<String, Long> result = analyticsService.getTopJobCategories(AUTH);

        // ASSERT
        assertNotNull(result);
        assertEquals(2L, result.get("IT"));   // 2 IT jobs
        assertEquals(1L, result.get("Design")); // 1 Design job
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — getTopJobCategories() — Empty List ✅
    // Kya test: Koi job nahi toh empty map
    // ════════════════════════════════════════════════════════════
    @Test
    void getTopJobCategories_WhenNoJobs_ShouldReturnEmptyMap() {
        // ARRANGE
        when(jobClient.getAllJobs(AUTH)).thenReturn(emptyResponse);

        // ACT
        Map<String, Long> result = analyticsService.getTopJobCategories(AUTH);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — getPlatformStats() — Total Stats ✅
    // Kya test: Platform level stats sahi hon
    // Expected: totalJobs=2, totalApplications=3
    // ════════════════════════════════════════════════════════════
    @Test
    void getPlatformStats_ShouldReturnCorrectTotals() {
        // ARRANGE
        ApiResponse allJobsResponse = new ApiResponse("success", Arrays.asList(job1, job2));
        when(jobClient.getAllJobs(AUTH)).thenReturn(allJobsResponse);

        // job1 ke 3 applications, job2 ke 0
        when(applicationClient.getApplicationsByJob(1L, AUTH)).thenReturn(appsResponse);
        when(applicationClient.getApplicationsByJob(2L, AUTH)).thenReturn(emptyResponse);
        when(interviewClient.getByApplication(any(), any())).thenReturn(null);

        // ACT
        AnalyticsSummary result = analyticsService.getPlatformStats(AUTH);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.getTotalJobs());
        assertEquals(3, result.getTotalApplications());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — getPlatformStats() — Status Counts ✅
    // Kya test: SHORTLISTED, OFFERED, REJECTED counts sahi hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getPlatformStats_ShouldReturnCorrectStatusCounts() {
        // ARRANGE
        ApiResponse allJobsResponse = new ApiResponse("success", List.of(job1));
        when(jobClient.getAllJobs(AUTH)).thenReturn(allJobsResponse);
        when(applicationClient.getApplicationsByJob(1L, AUTH)).thenReturn(appsResponse);
        when(interviewClient.getByApplication(any(), any())).thenReturn(null);

        // ACT
        AnalyticsSummary result = analyticsService.getPlatformStats(AUTH);

        // ASSERT
        assertEquals(1, result.getShortlistedCount()); // 1 SHORTLISTED
        assertEquals(1, result.getOfferedCount());     // 1 OFFERED
        assertEquals(1, result.getRejectedCount());    // 1 REJECTED
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — getPipelineStats() — Recruiter Specific ✅
    // Kya test: Ek recruiter ke jobs aur applications ki stats
    // ════════════════════════════════════════════════════════════
    @Test
    void getPipelineStats_ShouldReturnRecruiterSpecificStats() {
        // ARRANGE — recruiter profile mock
        RecruiterProfileDTO recruiter = new RecruiterProfileDTO();
        recruiter.setProfileId(5L);
        recruiter.setEmail("recruiter1@gmail.com");
        recruiter.setFullName("Pooja Sharma");

        ApiResponse recruiterResponse = new ApiResponse("success", recruiter);
        when(profileClient.getRecruiterById(5L, AUTH)).thenReturn(recruiterResponse);

        // recruiter1 ke 2 jobs
        ApiResponse allJobsResponse = new ApiResponse("success", Arrays.asList(job1, job2));
        when(jobClient.getAllJobs(AUTH)).thenReturn(allJobsResponse);

        // job1 ke 3 apps, job2 ke 0
        when(applicationClient.getApplicationsByJob(1L, AUTH)).thenReturn(appsResponse);
        when(applicationClient.getApplicationsByJob(2L, AUTH)).thenReturn(emptyResponse);
        when(interviewClient.getByApplication(any(), any())).thenReturn(null);

        // ACT
        AnalyticsSummary result = analyticsService.getPipelineStats(5L, AUTH);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.getTotalJobs());        // 2 jobs recruiter ke
        assertEquals(3, result.getTotalApplications()); // 3 total apps
        assertEquals(1, result.getShortlistedCount()); // 1 shortlisted
        assertEquals(1, result.getOfferedCount());     // 1 offered
        assertEquals(1, result.getRejectedCount());    // 1 rejected
    }
}