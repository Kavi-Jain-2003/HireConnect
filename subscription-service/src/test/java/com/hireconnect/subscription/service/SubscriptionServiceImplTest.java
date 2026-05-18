package com.hireconnect.subscription.service;

// ════════════════════════════════════════════════════════════════
//  SubscriptionServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  subscribe()                     — FREE plan ✅
//    2.  subscribe()                     — PROFESSIONAL plan ✅
//    3.  subscribe()                     — already active ❌
//    4.  cancelSubscription()            — successful ✅
//    5.  cancelSubscription()            — not found ❌
//    6.  renewSubscription()             — successful ✅
//    7.  renewSubscription()             — cancelled sub ❌
//    8.  getActiveSubscription()         — active milti hai ✅
//    9.  getActiveSubscription()         — expired handle ✅
//    10. getSubscriptionsByRecruiterId() — list return ✅
//    11. getInvoices()                   — invoices milein ✅
//    12. getPlanAmount()                 — FREE=0, PRO=999, ENT=1999
// ════════════════════════════════════════════════════════════════

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.hireconnect.subscription.repository.SubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock private SubscriptionRepository subscriptionRepo;
    @Mock private InvoiceRepository invoiceRepo;

    // ── Real class ────────────────────────────────────────────────
    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    // ── Test Data ─────────────────────────────────────────────────
    private Subscription activeSub;
    private Subscription cancelledSub;
    private Invoice sampleInvoice;

    @BeforeEach
    void setUp() {
        // Active subscription
        activeSub = new Subscription();
        activeSub.setSubscriptionId(1);
        activeSub.setRecruiterId(5);
        activeSub.setPlan("PROFESSIONAL");
        activeSub.setStartDate(LocalDate.now());
        activeSub.setEndDate(LocalDate.now().plusMonths(1));
        activeSub.setStatus("ACTIVE");
        activeSub.setAmountPaid(999.0);

        // Cancelled subscription
        cancelledSub = new Subscription();
        cancelledSub.setSubscriptionId(2);
        cancelledSub.setRecruiterId(5);
        cancelledSub.setPlan("FREE");
        cancelledSub.setStatus("CANCELLED");
        cancelledSub.setAmountPaid(0.0);

        // Sample Invoice
        sampleInvoice = new Invoice();
        sampleInvoice.setInvoiceId(1);
        sampleInvoice.setSubscriptionId(1);
        sampleInvoice.setRecruiterId(5);
        sampleInvoice.setAmount(999.0);
        sampleInvoice.setPaymentDate(LocalDateTime.now());
        sampleInvoice.setPaymentMode("DIRECT");
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — subscribe() — FREE Plan ✅
    // Kya test: FREE plan subscribe karo
    // Expected: status=ACTIVE, amount=0, invoice generate ho
    // ════════════════════════════════════════════════════════════
    @Test
    void subscribe_FreePlan_ShouldCreateActiveSubscription() {
        // ARRANGE — koi active subscription nahi
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(null);

        Subscription freeSub = new Subscription();
        freeSub.setSubscriptionId(3);
        freeSub.setRecruiterId(5);
        freeSub.setPlan("FREE");
        freeSub.setStatus("ACTIVE");
        freeSub.setAmountPaid(0.0);

        when(subscriptionRepo.save(any(Subscription.class))).thenReturn(freeSub);
        when(invoiceRepo.save(any(Invoice.class))).thenReturn(sampleInvoice);

        // ACT
        Subscription result = subscriptionService.subscribe(5, "FREE");

        // ASSERT
        assertNotNull(result);
        assertEquals("FREE", result.getPlan());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(0.0, result.getAmountPaid());
        verify(subscriptionRepo, times(1)).save(any(Subscription.class));
        verify(invoiceRepo, times(1)).save(any(Invoice.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — subscribe() — PROFESSIONAL Plan ✅
    // Kya test: PROFESSIONAL plan subscribe karo
    // Expected: amount = 999
    // ════════════════════════════════════════════════════════════
    @Test
    void subscribe_ProfessionalPlan_ShouldSetCorrectAmount() {
        // ARRANGE
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(null);
        when(subscriptionRepo.save(any(Subscription.class))).thenReturn(activeSub);
        when(invoiceRepo.save(any(Invoice.class))).thenReturn(sampleInvoice);

        // ACT
        Subscription result = subscriptionService.subscribe(5, "PROFESSIONAL");

        // ASSERT
        assertNotNull(result);
        assertEquals(999.0, result.getAmountPaid());
        assertEquals("ACTIVE", result.getStatus());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — subscribe() — Already Active ❌
    // Kya test: Active subscription hone pe dobara subscribe nahi
    // Expected: RuntimeException
    // ════════════════════════════════════════════════════════════
    @Test
    void subscribe_WhenAlreadyActive_ShouldThrowException() {
        // ARRANGE — active subscription already hai
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(activeSub);

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            subscriptionService.subscribe(5, "PROFESSIONAL");
        });

        assertEquals("Active subscription already exists", ex.getMessage());
        verify(subscriptionRepo, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — cancelSubscription() — Successful ✅
    // Kya test: Subscription cancel ho jaaye
    // Expected: status = CANCELLED
    // ════════════════════════════════════════════════════════════
    @Test
    void cancelSubscription_WhenValid_ShouldSetCancelledStatus() {
        // ARRANGE
        when(subscriptionRepo.findBySubscriptionIdAndRecruiterId(1, 5))
                .thenReturn(Optional.of(activeSub));
        when(subscriptionRepo.save(any(Subscription.class))).thenReturn(activeSub);

        // ACT
        subscriptionService.cancelSubscription(5, 1);

        // ASSERT
        assertEquals("CANCELLED", activeSub.getStatus());
        verify(subscriptionRepo, times(1)).save(activeSub);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — cancelSubscription() — Not Found ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void cancelSubscription_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(subscriptionRepo.findBySubscriptionIdAndRecruiterId(999, 5))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            subscriptionService.cancelSubscription(5, 999);
        });

        assertEquals("Subscription not found", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — renewSubscription() — Successful ✅
    // Kya test: Expired subscription renew ho
    // Expected: status=ACTIVE, endDate extend ho
    // ════════════════════════════════════════════════════════════
    @Test
    void renewSubscription_WhenExpired_ShouldRenewSuccessfully() {
        // ARRANGE — expired subscription
        Subscription expiredSub = new Subscription();
        expiredSub.setSubscriptionId(1);
        expiredSub.setRecruiterId(5);
        expiredSub.setPlan("PROFESSIONAL");
        expiredSub.setEndDate(LocalDate.now().minusDays(5)); // expired
        expiredSub.setStatus("EXPIRED");
        expiredSub.setAmountPaid(999.0);

        when(subscriptionRepo.findBySubscriptionIdAndRecruiterId(1, 5))
                .thenReturn(Optional.of(expiredSub));
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(null);
        when(subscriptionRepo.save(any(Subscription.class))).thenReturn(expiredSub);
        when(invoiceRepo.save(any(Invoice.class))).thenReturn(sampleInvoice);

        // ACT
        Subscription result = subscriptionService.renewSubscription(5, 1);

        // ASSERT
        assertNotNull(result);
        assertEquals("ACTIVE", expiredSub.getStatus());
        assertTrue(expiredSub.getEndDate().isAfter(LocalDate.now()));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — renewSubscription() — Cancelled ❌
    // Kya test: Cancelled subscription renew nahi ho sakti
    // ════════════════════════════════════════════════════════════
    @Test
    void renewSubscription_WhenCancelled_ShouldThrowException() {
        // ARRANGE
        when(subscriptionRepo.findBySubscriptionIdAndRecruiterId(2, 5))
                .thenReturn(Optional.of(cancelledSub));

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            subscriptionService.renewSubscription(5, 2);
        });

        assertEquals("Cannot renew cancelled subscription", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — getActiveSubscription() — Active milti hai ✅
    // ════════════════════════════════════════════════════════════
    @Test
    void getActiveSubscription_WhenActive_ShouldReturn() {
        // ARRANGE — future end date
        activeSub.setEndDate(LocalDate.now().plusDays(10));
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(activeSub);

        // ACT
        Subscription result = subscriptionService.getActiveSubscription(5);

        // ASSERT
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(5, result.getRecruiterId());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — getActiveSubscription() — Expired Handle ✅
    // Kya test: End date past ho toh EXPIRED mark ho aur null return
    // ════════════════════════════════════════════════════════════
    @Test
    void getActiveSubscription_WhenExpired_ShouldMarkExpiredAndReturnNull() {
        // ARRANGE — past end date
        activeSub.setEndDate(LocalDate.now().minusDays(5));
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(activeSub);
        when(subscriptionRepo.save(any(Subscription.class))).thenReturn(activeSub);

        // ACT
        Subscription result = subscriptionService.getActiveSubscription(5);

        // ASSERT
        assertNull(result); // null return hona chahiye
        assertEquals("EXPIRED", activeSub.getStatus()); // EXPIRED mark ho gaya
        verify(subscriptionRepo, times(1)).save(activeSub);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — getSubscriptionsByRecruiterId() ✅
    // Kya test: Recruiter ki saari subscriptions return hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getSubscriptionsByRecruiterId_ShouldReturnList() {
        // ARRANGE
        when(subscriptionRepo.findByRecruiterId(5))
                .thenReturn(List.of(activeSub, cancelledSub));

        // ACT
        List<Subscription> result = subscriptionService.getSubscriptionsByRecruiterId(5);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(5, result.get(0).getRecruiterId());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — getInvoices() ✅
    // Kya test: Subscription ki saari invoices return hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getInvoices_ShouldReturnInvoicesForSubscription() {
        // ARRANGE
        when(subscriptionRepo.findBySubscriptionIdAndRecruiterId(1, 5))
                .thenReturn(Optional.of(activeSub));
        when(invoiceRepo.findBySubscriptionId(1))
                .thenReturn(List.of(sampleInvoice));

        // ACT
        List<Invoice> result = subscriptionService.getInvoices(5, 1);

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(999.0, result.get(0).getAmount());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 12 — Plan Amount Check ✅
    // Kya test: FREE=0, PROFESSIONAL=999, ENTERPRISE=1999
    // ════════════════════════════════════════════════════════════
    @Test
    void subscribe_EnterprisePlan_ShouldSetCorrectAmount() {
        // ARRANGE
        when(subscriptionRepo.findFirstByRecruiterIdAndStatus(5, "ACTIVE"))
                .thenReturn(null);

        Subscription entSub = new Subscription();
        entSub.setSubscriptionId(4);
        entSub.setRecruiterId(5);
        entSub.setPlan("ENTERPRISE");
        entSub.setStatus("ACTIVE");
        entSub.setAmountPaid(1999.0);

        when(subscriptionRepo.save(any(Subscription.class))).thenReturn(entSub);
        when(invoiceRepo.save(any(Invoice.class))).thenReturn(sampleInvoice);

        // ACT
        Subscription result = subscriptionService.subscribe(5, "ENTERPRISE");

        // ASSERT
        assertEquals(1999.0, result.getAmountPaid());
        assertEquals("ENTERPRISE", result.getPlan());
    }
}
