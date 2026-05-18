package com.hireconnect.notification.service;

// ════════════════════════════════════════════════════════════════
//  NotificationServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  sendNotification()      — successful save
//    2.  sendNotification()      — null notification ❌
//    3.  getByUser()             — user ki notifications
//    4.  getByUser()             — empty list
//    5.  markAsRead()            — notification read mark ho
//    6.  markAsRead()            — not found ❌
//    7.  markAllRead()           — sab unread mark ho
//    8.  deleteNotification()    — successful delete
//    9.  deleteNotification()    — not found ❌
//    10. getUnreadCount()        — unread count
//    11. dispatchNotification()  — in-app + email dispatch
//    12. notifyJobAlert()        — JOB_ALERT type
// ════════════════════════════════════════════════════════════════

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.repository.NotificationRepository;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock
    private NotificationRepository repo;

    @Mock
    private EmailService emailService;

    // ── Real class ────────────────────────────────────────────────
    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ── Test Data ─────────────────────────────────────────────────
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification();
        sampleNotification.setNotificationId(1L);
        sampleNotification.setUserId(3L);
        sampleNotification.setType("APPLICATION_STATUS");
        sampleNotification.setMessage("Your application has been shortlisted.");
        sampleNotification.setRead(false);
        sampleNotification.setCreatedAt(LocalDateTime.now());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — sendNotification() — Successful ✅
    // Kya test: Notification successfully save ho jaaye
    // Expected: saved notification return ho
    // ════════════════════════════════════════════════════════════
    @Test
    void sendNotification_WhenValid_ShouldSaveAndReturn() {
        // ARRANGE
        when(repo.save(any(Notification.class))).thenReturn(sampleNotification);

        // ACT
        Notification result = notificationService.sendNotification(sampleNotification);

        // ASSERT
        assertNotNull(result);
        assertEquals(3L, result.getUserId());
        assertEquals("APPLICATION_STATUS", result.getType());
        assertFalse(result.isRead()); // nai notification unread hoti hai
        verify(repo, times(1)).save(any(Notification.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — sendNotification() — Null ❌
    // Kya test: Null notification pe exception aaye
    // Expected: RuntimeException
    // ════════════════════════════════════════════════════════════
    @Test
    void sendNotification_WhenNull_ShouldThrowException() {
        // ACT + ASSERT
        assertThrows(RuntimeException.class, () -> {
            notificationService.sendNotification(null);
        });

        // Save call nahi hona chahiye
        verify(repo, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — getByUser() — Notifications milein ✅
    // Kya test: User ki saari notifications return hon
    // Expected: 2 notifications
    // ════════════════════════════════════════════════════════════
    @Test
    void getByUser_ShouldReturnUserNotifications() {
        // ARRANGE
        Notification n2 = new Notification();
        n2.setNotificationId(2L);
        n2.setUserId(3L);
        n2.setType("JOB_ALERT");
        n2.setMessage("New job posted: Java Developer");

        when(repo.findByUserId(3L)).thenReturn(List.of(sampleNotification, n2));

        // ACT
        List<Notification> result = notificationService.getByUser(3L);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getUserId());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — getByUser() — Empty List
    // Kya test: Koi notification nahi toh empty list aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void getByUser_WhenNoNotifications_ShouldReturnEmptyList() {
        // ARRANGE
        when(repo.findByUserId(99L)).thenReturn(Collections.emptyList());

        // ACT
        List<Notification> result = notificationService.getByUser(99L);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — markAsRead() — Successful ✅
    // Kya test: Notification read mark ho jaaye
    // ════════════════════════════════════════════════════════════
    @Test
    void markAsRead_WhenExists_ShouldMarkAsRead() {
        // ARRANGE
        sampleNotification.setRead(false);
        when(repo.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(repo.save(any(Notification.class))).thenReturn(sampleNotification);

        // ACT
        notificationService.markAsRead(1L);

        // ASSERT
        assertTrue(sampleNotification.isRead()); // read = true ho gaya
        verify(repo, times(1)).save(sampleNotification);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — markAsRead() — Not Found ❌
    // Kya test: Wrong ID pe exception aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void markAsRead_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(repo.findById(999L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(999L);
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — markAllRead() — Sab Unread ✅
    // Kya test: User ki saari unread notifications read ho jayein
    // ════════════════════════════════════════════════════════════
    @Test
    void markAllRead_ShouldMarkAllUnreadAsRead() {
        // ARRANGE
        Notification n2 = new Notification();
        n2.setNotificationId(2L);
        n2.setUserId(3L);
        n2.setRead(false);

        List<Notification> unreadList = List.of(sampleNotification, n2);
        when(repo.findByUserIdAndIsRead(3L, false)).thenReturn(unreadList);
        when(repo.saveAll(anyList())).thenReturn(unreadList);

        // ACT
        notificationService.markAllRead(3L);

        // ASSERT
        assertTrue(sampleNotification.isRead()); // dono read ho gaye
        assertTrue(n2.isRead());
        verify(repo, times(1)).saveAll(unreadList);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — deleteNotification() — Successful ✅
    // Kya test: Notification successfully delete ho
    // ════════════════════════════════════════════════════════════
    @Test
    void deleteNotification_WhenExists_ShouldDelete() {
        // ARRANGE
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteByNotificationId(1L);

        // ACT
        notificationService.deleteNotification(1L);

        // ASSERT
        verify(repo, times(1)).deleteByNotificationId(1L);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — deleteNotification() — Not Found ❌
    // Kya test: Wrong ID pe exception aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void deleteNotification_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(repo.existsById(999L)).thenReturn(false);

        // ACT + ASSERT
        assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification(999L);
        });

        // delete call nahi hona chahiye
        verify(repo, never()).deleteByNotificationId(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — getUnreadCount() ✅
    // Kya test: Unread notifications ki count sahi aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        // ARRANGE
        when(repo.countByUserIdAndIsRead(3L, false)).thenReturn(5L);

        // ACT
        long count = notificationService.getUnreadCount(3L);

        // ASSERT
        assertEquals(5L, count);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — dispatchNotification() — In-app + Email ✅
    // Kya test: Notification DB mein save ho aur email bhi jaaye
    // ════════════════════════════════════════════════════════════
    @Test
    void dispatchNotification_ShouldSaveAndSendEmail() {
        // ARRANGE
        when(repo.findFirstByUserIdAndTypeAndMessageOrderByCreatedAtDesc(
                anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
        when(repo.save(any(Notification.class))).thenReturn(sampleNotification);
        doNothing().when(emailService).sendSimple(anyString(), anyString(), anyString());

        // ACT
        Notification result = notificationService.dispatchNotification(
                3L,
                "APPLICATION_STATUS",
                "Your application has been shortlisted.",
                "candidate1@gmail.com",
                "Application Shortlisted"
        );

        // ASSERT
        assertNotNull(result);
        verify(repo, times(1)).save(any(Notification.class));
        // Email bhi send hua
        verify(emailService, times(1)).sendSimple(
                eq("candidate1@gmail.com"),
                anyString(),
                anyString()
        );
    }

    // ════════════════════════════════════════════════════════════
    // TEST 12 — notifyJobAlert() ✅
    // Kya test: JOB_ALERT type notification save ho
    // ════════════════════════════════════════════════════════════
    @Test
    void notifyJobAlert_ShouldSaveWithJobAlertType() {
        // ARRANGE
        Notification jobAlert = new Notification();
        jobAlert.setNotificationId(3L);
        jobAlert.setUserId(3L);
        jobAlert.setType("JOB_ALERT");
        jobAlert.setMessage("New job posted: Java Developer in Agra.");

        when(repo.findFirstByUserIdAndTypeAndMessageOrderByCreatedAtDesc(
                anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
        when(repo.save(any(Notification.class))).thenReturn(jobAlert);

        // ACT
        Notification result = notificationService.notifyJobAlert(
                3L,
                "New job posted: Java Developer in Agra."
        );

        // ASSERT
        assertNotNull(result);
        assertEquals("JOB_ALERT", result.getType());
        assertEquals(3L, result.getUserId());
        verify(repo, times(1)).save(any(Notification.class));
    }
}
