package com.hireconnect.interview.service;

// ════════════════════════════════════════════════════════════════
//  InterviewServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  scheduleInterview()    — naya interview schedule ho ✅
//    2.  scheduleInterview()    — existing interview reschedule ho ✅
//    3.  confirmInterview()     — status CONFIRMED ho ✅
//    4.  confirmInterview()     — not found ❌
//    5.  rescheduleInterview()  — new time set ho ✅
//    6.  rescheduleInterview()  — not found ❌
//    7.  cancelInterview()      — status CANCELLED ho ✅
//    8.  cancelInterview()      — not found ❌
//    9.  getByApplication()     — application ki interviews ✅
//    10. getByApplication()     — empty list ✅
//    11. getByStatus()          — status se filter ✅
// ════════════════════════════════════════════════════════════════

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.interview.client.ApplicationClient;
import com.hireconnect.interview.client.AuthClient;
import com.hireconnect.interview.client.JobClient;
import com.hireconnect.interview.client.NotificationClient;
import com.hireconnect.interview.client.ProfileClient;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.messaging.NotificationEventPublisher;
import com.hireconnect.interview.repository.InterviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock private InterviewRepository repository;
    @Mock private ApplicationClient applicationClient;
    @Mock private ProfileClient profileClient;
    @Mock private JobClient jobClient;
    @Mock private AuthClient authClient;
    @Mock private NotificationClient notificationClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private NotificationEventPublisher eventPublisher;

    // ── Real class ────────────────────────────────────────────────
    @InjectMocks
    private InterviewServiceImpl interviewService;

    // ── Test Data ─────────────────────────────────────────────────
    private Interview sampleInterview;

    @BeforeEach
    void setUp() {
        sampleInterview = new Interview();
        sampleInterview.setInterviewId(1L);
        sampleInterview.setApplicationId(10L);
        sampleInterview.setScheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        sampleInterview.setMode("Online");
        sampleInterview.setMeetLink("https://meet.google.com/abc");
        sampleInterview.setStatus("SCHEDULED");
        sampleInterview.setNotes("Be ready with your resume");
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — scheduleInterview() — Naya Interview ✅
    // Kya test: Pehli baar interview schedule ho
    // Expected: status = SCHEDULED, save call hua
    // ════════════════════════════════════════════════════════════
    @Test
    void scheduleInterview_WhenNew_ShouldSaveWithScheduledStatus() {
        // ARRANGE — koi existing interview nahi
        when(repository.findFirstByApplicationIdOrderByInterviewIdDesc(10L))
                .thenReturn(Optional.empty());
        when(repository.save(any(Interview.class))).thenReturn(sampleInterview);

        // Notification ke liye mock — null return karo (ignore notification)
        when(applicationClient.getApplicationById(any())).thenReturn(null);

        // ACT
        Interview result = interviewService.scheduleInterview(sampleInterview);

        // ASSERT
        assertNotNull(result);
        assertEquals("SCHEDULED", result.getStatus());
        verify(repository, times(1)).save(any(Interview.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — scheduleInterview() — Existing Reschedule ✅
    // Kya test: Pehle se interview hai toh update ho
    // Expected: existing interview update ho
    // ════════════════════════════════════════════════════════════
    @Test
    void scheduleInterview_WhenExisting_ShouldUpdateExisting() {
        // ARRANGE — existing interview hai
        Interview existing = new Interview();
        existing.setInterviewId(1L);
        existing.setApplicationId(10L);
        existing.setScheduledAt(LocalDateTime.of(2026, 5, 1, 10, 0)); // old time
        existing.setMode("Online");
        existing.setStatus("SCHEDULED");

        when(repository.findFirstByApplicationIdOrderByInterviewIdDesc(10L))
                .thenReturn(Optional.of(existing));

        // New interview with different time
        Interview newInterview = new Interview();
        newInterview.setApplicationId(10L);
        newInterview.setScheduledAt(LocalDateTime.of(2026, 6, 15, 14, 0)); // new time
        newInterview.setMode("In-Person");
        newInterview.setNotes("Updated notes");

        when(repository.save(any(Interview.class))).thenReturn(existing);
        when(applicationClient.getApplicationById(any())).thenReturn(null);

        // ACT
        Interview result = interviewService.scheduleInterview(newInterview);

        // ASSERT
        assertNotNull(result);
        assertEquals("SCHEDULED", existing.getStatus());
        verify(repository, times(1)).save(existing); // existing update hua
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — confirmInterview() — Successful ✅
    // Kya test: Interview confirm ho jaaye
    // Expected: status = CONFIRMED
    // ════════════════════════════════════════════════════════════
    @Test
    void confirmInterview_WhenExists_ShouldSetConfirmedStatus() {
        // ARRANGE
        when(repository.findById(1L)).thenReturn(Optional.of(sampleInterview));
        when(repository.save(any(Interview.class))).thenReturn(sampleInterview);
        when(applicationClient.getApplicationById(any())).thenReturn(null);

        // ACT
        Interview result = interviewService.confirmInterview(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals("CONFIRMED", sampleInterview.getStatus());
        verify(repository, times(1)).save(sampleInterview);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — confirmInterview() — Not Found ❌
    // Kya test: Wrong ID pe exception aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void confirmInterview_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(Exception.class, () -> {
            interviewService.confirmInterview(999L);
        });

        verify(repository, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — rescheduleInterview() — Successful ✅
    // Kya test: Interview ka time change ho
    // Expected: status = RESCHEDULED, new time set
    // ════════════════════════════════════════════════════════════
    @Test
    void rescheduleInterview_WhenExists_ShouldUpdateTimeAndStatus() {
        // ARRANGE
        LocalDateTime newTime = LocalDateTime.of(2026, 7, 10, 15, 30);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleInterview));
        when(repository.save(any(Interview.class))).thenReturn(sampleInterview);
        when(applicationClient.getApplicationById(any())).thenReturn(null);

        // ACT
        Interview result = interviewService.rescheduleInterview(1L, newTime);

        // ASSERT
        assertNotNull(result);
        assertEquals("RESCHEDULED", sampleInterview.getStatus());
        assertEquals(newTime, sampleInterview.getScheduledAt()); // time change hua
        verify(repository, times(1)).save(sampleInterview);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — rescheduleInterview() — Not Found ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void rescheduleInterview_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(Exception.class, () -> {
            interviewService.rescheduleInterview(999L,
                    LocalDateTime.of(2026, 7, 10, 15, 30));
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — cancelInterview() — Successful ✅
    // Kya test: Interview cancel ho jaaye
    // Expected: status = CANCELLED
    // ════════════════════════════════════════════════════════════
    @Test
    void cancelInterview_WhenExists_ShouldSetCancelledStatus() {
        // ARRANGE
        when(repository.findById(1L)).thenReturn(Optional.of(sampleInterview));
        when(repository.save(any(Interview.class))).thenReturn(sampleInterview);
        when(applicationClient.getApplicationById(any())).thenReturn(null);

        // ACT
        interviewService.cancelInterview(1L);

        // ASSERT
        assertEquals("CANCELLED", sampleInterview.getStatus());
        verify(repository, times(1)).save(sampleInterview);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — cancelInterview() — Not Found ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void cancelInterview_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(Exception.class, () -> {
            interviewService.cancelInterview(999L);
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — getByApplication() — Interviews milein ✅
    // Kya test: Application ID se saari interviews return hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getByApplication_ShouldReturnInterviews() {
        // ARRANGE
        Interview i2 = new Interview();
        i2.setInterviewId(2L);
        i2.setApplicationId(10L);
        i2.setStatus("CONFIRMED");

        when(repository.findByApplicationId(10L))
                .thenReturn(List.of(sampleInterview, i2));

        // ACT
        List<Interview> result = interviewService.getByApplication(10L);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getApplicationId());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — getByApplication() — Empty List ✅
    // Kya test: Koi interview nahi toh empty list
    // ════════════════════════════════════════════════════════════
    @Test
    void getByApplication_WhenNone_ShouldReturnEmptyList() {
        // ARRANGE
        when(repository.findByApplicationId(99L))
                .thenReturn(Collections.emptyList());

        // ACT
        List<Interview> result = interviewService.getByApplication(99L);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — getByStatus() — Status se filter ✅
    // Kya test: CONFIRMED status ki saari interviews
    // ════════════════════════════════════════════════════════════
    @Test
    void getByStatus_ShouldReturnFilteredInterviews() {
        // ARRANGE
        Interview confirmed = new Interview();
        confirmed.setInterviewId(2L);
        confirmed.setApplicationId(10L);
        confirmed.setStatus("CONFIRMED");

        when(repository.findByStatus("CONFIRMED"))
                .thenReturn(List.of(confirmed));

        // ACT
        List<Interview> result = interviewService.getByStatus("CONFIRMED");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CONFIRMED", result.get(0).getStatus());
    }
}
