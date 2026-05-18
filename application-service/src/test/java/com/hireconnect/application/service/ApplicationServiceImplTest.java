package com.hireconnect.application.service;

// ════════════════════════════════════════════════════════════════
//  ApplicationServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  updateStatus() — APPLIED → SHORTLISTED  ✅
//    2.  updateStatus() — APPLIED → OFFERED ❌ invalid transition
//    3.  updateStatus() — SHORTLISTED → INTERVIEW_SCHEDULED ✅
//    4.  updateStatus() — already OFFERED → change ❌ finalized
//    5.  updateStatus() — already REJECTED → change ❌ finalized
//    6.  updateStatus() — APPLIED → REJECTED ❌ invalid transition
//    7.  getByJob()     — job ki saari applications return hon
//    8.  getApplicationById() — found ✅
//    9.  getApplicationById() — not found ❌
//    10. withdrawApplication() — successful ✅
//    11. withdrawApplication() — already withdrawn ❌
//    12. withdrawApplication() — after OFFERED ❌
// ════════════════════════════════════════════════════════════════

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.application.client.AuthClient;
import com.hireconnect.application.client.InterviewClient;
import com.hireconnect.application.client.JobClient;
import com.hireconnect.application.client.NotificationClient;
import com.hireconnect.application.client.ProfileClient;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.UpdateStatusRequest;
import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.exception.BusinessException;
import com.hireconnect.application.exception.ResourceNotFoundException;
import com.hireconnect.application.messaging.NotificationEventPublisher;
import com.hireconnect.application.repository.ApplicationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock private ApplicationRepository repository;
    @Mock private JobClient jobClient;
    @Mock private ProfileClient profileClient;
    @Mock private AuthClient authClient;
    @Mock private InterviewClient interviewClient;
    @Mock private NotificationClient notificationClient;
    @Mock private NotificationEventPublisher eventPublisher;
    @Mock private ObjectMapper objectMapper;

    // ── Real class ────────────────────────────────────────────────
    @InjectMocks
    private ApplicationServiceImpl applicationService;

    // ── Test Data ─────────────────────────────────────────────────
    private Application sampleApplication;
    private UpdateStatusRequest updateStatusRequest;

    @BeforeEach
    void setUp() {
        // Sample application — jaise DB mein hoti hai
        sampleApplication = new Application();
        sampleApplication.setApplicationId(1L);
        sampleApplication.setJobId(10L);
        sampleApplication.setCandidateId(5L);
        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        sampleApplication.setAppliedAt(LocalDateTime.now());
        sampleApplication.setCoverLetter("I am interested in this job");

        // UpdateStatus request
        updateStatusRequest = new UpdateStatusRequest();
    }

    // ── Helper: SecurityContext mock karo ─────────────────────────
    // Har test mein role set karne ke liye
    private void mockSecurityContext(String role) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        // Role set karo
        GrantedAuthority authority = new SimpleGrantedAuthority(role);
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        when(authentication.getPrincipal()).thenReturn("recruiter1@gmail.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — updateStatus() APPLIED → SHORTLISTED ✅
    // Kya test: Valid transition — recruiter shortlist kar sake
    // Expected: "Application status updated successfully"
    // ════════════════════════════════════════════════════════════
    @Test
    void updateStatus_AppliedToShortlisted_ShouldSucceed() {
        // ARRANGE — recruiter role set karo
        mockSecurityContext("ROLE_RECRUITER");

        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        updateStatusRequest.setStatus(ApplicationStatus.SHORTLISTED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(repository.save(any(Application.class))).thenReturn(sampleApplication);

        // Notification ke liye mock
        when(notificationClient.dispatch(any())).thenReturn(null);

        // ACT
        ApplicationResponse response = applicationService.updateStatus(1L, updateStatusRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals("Application status updated successfully", response.getMessage());
        assertEquals(ApplicationStatus.SHORTLISTED, sampleApplication.getStatus());
        verify(repository, times(1)).save(sampleApplication);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — updateStatus() APPLIED → OFFERED ❌
    // Kya test: Invalid transition — directly offer nahi de sakte
    // Expected: BusinessException
    // ════════════════════════════════════════════════════════════
    @Test
    void updateStatus_AppliedToOffered_ShouldThrowException() {
        // ARRANGE
        mockSecurityContext("ROLE_RECRUITER");

        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        updateStatusRequest.setStatus(ApplicationStatus.OFFERED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        // ACT + ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.updateStatus(1L, updateStatusRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        // Save NAHI hona chahiye
        verify(repository, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — updateStatus() SHORTLISTED → INTERVIEW_SCHEDULED ✅
    // Kya test: Valid transition
    // ════════════════════════════════════════════════════════════
    @Test
    void updateStatus_ShortlistedToInterviewScheduled_ShouldSucceed() {
        // ARRANGE
        mockSecurityContext("ROLE_RECRUITER");

        sampleApplication.setStatus(ApplicationStatus.SHORTLISTED);
        updateStatusRequest.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));
        when(repository.save(any(Application.class))).thenReturn(sampleApplication);
        when(notificationClient.dispatch(any())).thenReturn(null);

        // ACT
        ApplicationResponse response = applicationService.updateStatus(1L, updateStatusRequest);

        // ASSERT
        assertEquals("Application status updated successfully", response.getMessage());
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, sampleApplication.getStatus());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — updateStatus() Already OFFERED ❌
    // Kya test: Final state ke baad change nahi ho sakta
    // Expected: BusinessException — "Application already finalized"
    // ════════════════════════════════════════════════════════════
    @Test
    void updateStatus_WhenAlreadyOffered_ShouldThrowException() {
        // ARRANGE
        mockSecurityContext("ROLE_RECRUITER");

        sampleApplication.setStatus(ApplicationStatus.OFFERED); // already final
        updateStatusRequest.setStatus(ApplicationStatus.SHORTLISTED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        // ACT + ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.updateStatus(1L, updateStatusRequest);
        });

        assertEquals("Application already finalized", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — updateStatus() Already REJECTED ❌
    // Kya test: Rejected ke baad bhi change nahi ho sakta
    // ════════════════════════════════════════════════════════════
    @Test
    void updateStatus_WhenAlreadyRejected_ShouldThrowException() {
        // ARRANGE
        mockSecurityContext("ROLE_RECRUITER");

        sampleApplication.setStatus(ApplicationStatus.REJECTED);
        updateStatusRequest.setStatus(ApplicationStatus.SHORTLISTED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        // ACT + ASSERT
        assertThrows(BusinessException.class, () -> {
            applicationService.updateStatus(1L, updateStatusRequest);
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — updateStatus() APPLIED → REJECTED ❌
    // Kya test: APPLIED se directly REJECTED nahi ho sakta
    // ════════════════════════════════════════════════════════════
    @Test
    void updateStatus_AppliedToRejected_ShouldThrowException() {
        // ARRANGE
        mockSecurityContext("ROLE_RECRUITER");

        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        updateStatusRequest.setStatus(ApplicationStatus.REJECTED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        // ACT + ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.updateStatus(1L, updateStatusRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — getByJob() — Sab applications milein
    // Kya test: Job ID se saari applications return hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getByJob_ShouldReturnAllApplicationsForJob() {
        // ARRANGE
        Application app2 = new Application();
        app2.setApplicationId(2L);
        app2.setJobId(10L);
        app2.setCandidateId(6L);
        app2.setStatus(ApplicationStatus.SHORTLISTED);

        List<Application> apps = List.of(sampleApplication, app2);
        when(repository.findByJobId(10L)).thenReturn(apps);

        // ACT
        List<Application> result = applicationService.getByJob(10L);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getJobId());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — getApplicationById() — Found ✅
    // Kya test: ID se application milti hai
    // ════════════════════════════════════════════════════════════
    @Test
    void getApplicationById_WhenExists_ShouldReturnApplication() {
        // ARRANGE
        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        // ACT
        Application result = applicationService.getApplicationById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getApplicationId());
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — getApplicationById() — Not Found ❌
    // Kya test: Wrong ID pe exception aaye
    // Expected: ResourceNotFoundException
    // ════════════════════════════════════════════════════════════
    @Test
    void getApplicationById_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.getApplicationById(999L);
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — withdrawApplication() — Successful ✅
    // Kya test: Candidate apni application withdraw kar sake
    // ════════════════════════════════════════════════════════════
    @Test
    void withdrawApplication_WhenValid_ShouldWithdrawSuccessfully() {
        // ARRANGE — candidate role
        mockSecurityContext("ROLE_CANDIDATE");

        sampleApplication.setStatus(ApplicationStatus.APPLIED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        // Profile client mock — candidateId 5L return karo
        com.hireconnect.application.dto.ApiResponse profileResponse =
                mock(com.hireconnect.application.dto.ApiResponse.class);
        java.util.Map<String, Object> profileData = new java.util.HashMap<>();
        profileData.put("profileId", 5L);
        profileData.put("email", "candidate1@gmail.com");
        when(profileResponse.getData()).thenReturn(profileData);
        when(profileClient.getCandidateByEmail(anyString())).thenReturn(profileResponse);
        when(objectMapper.convertValue(any(), eq(java.util.Map.class))).thenReturn(profileData);
        when(repository.save(any(Application.class))).thenReturn(sampleApplication);

        // ACT
        ApplicationResponse response = applicationService.withdrawApplication(1L);

        // ASSERT
        assertEquals("Application withdrawn successfully", response.getMessage());
        assertEquals(ApplicationStatus.WITHDRAWN, sampleApplication.getStatus());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — withdrawApplication() — Already Withdrawn ❌
    // Kya test: Pehle se withdrawn application dobara withdraw nahi ho
    // ════════════════════════════════════════════════════════════
    @Test
    void withdrawApplication_WhenAlreadyWithdrawn_ShouldThrowException() {
        // ARRANGE
        mockSecurityContext("ROLE_CANDIDATE");

        sampleApplication.setStatus(ApplicationStatus.WITHDRAWN);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        com.hireconnect.application.dto.ApiResponse profileResponse =
                mock(com.hireconnect.application.dto.ApiResponse.class);
        java.util.Map<String, Object> profileData = new java.util.HashMap<>();
        profileData.put("profileId", 5L);
        profileData.put("email", "candidate1@gmail.com");
        when(profileResponse.getData()).thenReturn(profileData);
        when(profileClient.getCandidateByEmail(anyString())).thenReturn(profileResponse);
        when(objectMapper.convertValue(any(), eq(java.util.Map.class))).thenReturn(profileData);

        // ACT + ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.withdrawApplication(1L);
        });

        assertEquals("Application already withdrawn", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 12 — withdrawApplication() — After OFFERED ❌
    // Kya test: Offer ke baad withdraw nahi ho sakta
    // ════════════════════════════════════════════════════════════
    @Test
    void withdrawApplication_WhenOffered_ShouldThrowException() {
        // ARRANGE
        mockSecurityContext("ROLE_CANDIDATE");

        sampleApplication.setStatus(ApplicationStatus.OFFERED);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleApplication));

        com.hireconnect.application.dto.ApiResponse profileResponse =
                mock(com.hireconnect.application.dto.ApiResponse.class);
        java.util.Map<String, Object> profileData = new java.util.HashMap<>();
        profileData.put("profileId", 5L);
        profileData.put("email", "candidate1@gmail.com");
        when(profileResponse.getData()).thenReturn(profileData);
        when(profileClient.getCandidateByEmail(anyString())).thenReturn(profileResponse);
        when(objectMapper.convertValue(any(), eq(java.util.Map.class))).thenReturn(profileData);

        // ACT + ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.withdrawApplication(1L);
        });

        assertTrue(exception.getMessage().contains("Cannot withdraw after final decision"));
    }
}
