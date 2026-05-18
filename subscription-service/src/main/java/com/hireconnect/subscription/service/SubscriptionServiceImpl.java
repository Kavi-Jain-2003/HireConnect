package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final InvoiceRepository invoiceRepo;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepo,
                                   InvoiceRepository invoiceRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
    }

    // ─── Existing methods (unchanged) ───────────────────────────────────────

    @Override
    @Transactional
    public Subscription subscribe(int recruiterId, String plan) {
        Subscription existingActive = getActiveSubscription(recruiterId);
        if (existingActive != null) {
            throw new RuntimeException("Active subscription already exists");
        }

        Subscription sub = new Subscription();
        sub.setRecruiterId(recruiterId);
        sub.setPlan(plan == null ? "FREE" : plan.trim().toUpperCase());
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
        sub.setStatus("ACTIVE");
        sub.setAmountPaid(getPlanAmount(sub.getPlan()));

        Subscription saved = subscriptionRepo.save(sub);
        generateInvoice(saved.getSubscriptionId(), saved.getAmountPaid(), saved.getRecruiterId(), "DIRECT", UUID.randomUUID().toString());
        return saved;
    }

    @Override
    @Transactional
    public void cancelSubscription(int recruiterId, int subscriptionId) {
        Subscription sub = getOwnedSubscriptionOrThrow(recruiterId, subscriptionId);
        sub.setStatus("CANCELLED");
        subscriptionRepo.save(sub);
    }

    @Override
    @Transactional
    public Subscription renewSubscription(int recruiterId, int subscriptionId) {
        Subscription sub = getOwnedSubscriptionOrThrow(recruiterId, subscriptionId);

        if ("CANCELLED".equalsIgnoreCase(sub.getStatus())) {
            throw new RuntimeException("Cannot renew cancelled subscription");
        }

        Subscription existingActive = getActiveSubscription(recruiterId);
        if (existingActive != null && existingActive.getSubscriptionId() != subscriptionId) {
            throw new RuntimeException("Active subscription already exists");
        }

        LocalDate baseDate = sub.getEndDate();
        if (baseDate == null || baseDate.isBefore(LocalDate.now())) {
            baseDate = LocalDate.now();
        }

        sub.setStartDate(LocalDate.now());
        sub.setEndDate(baseDate.plusMonths(1));
        sub.setStatus("ACTIVE");

        Subscription saved = subscriptionRepo.save(sub);
        generateInvoice(saved.getSubscriptionId(), saved.getAmountPaid(), saved.getRecruiterId(), "RAZORPAY", UUID.randomUUID().toString());
        return saved;
    }

    @Override
    public Invoice generateInvoice(int subscriptionId, double amount) {
        return generateInvoice(subscriptionId, amount, 0, "ONLINE", UUID.randomUUID().toString());
    }

    private Invoice generateInvoice(int subscriptionId, double amount, int recruiterId, String paymentMode, String transactionId) {
        Invoice invoice = new Invoice();
        invoice.setSubscriptionId(subscriptionId);
        invoice.setRecruiterId(recruiterId);
        invoice.setAmount(amount);
        invoice.setPaymentDate(LocalDateTime.now());
        invoice.setPaymentMode(paymentMode);
        invoice.setTransactionId(transactionId);
        return invoiceRepo.save(invoice);
    }

    @Override
    public List<Invoice> getInvoices(int recruiterId, int subscriptionId) {
        getOwnedSubscriptionOrThrow(recruiterId, subscriptionId);
        return invoiceRepo.findBySubscriptionId(subscriptionId);
    }

    @Override
    public List<Subscription> getSubscriptionsByRecruiterId(int recruiterId) {
        return subscriptionRepo.findByRecruiterId(recruiterId);
    }

    @Override
    public Subscription getActiveSubscription(int recruiterId) {
        Subscription sub = subscriptionRepo.findFirstByRecruiterIdAndStatus(recruiterId, "ACTIVE");
        if (sub == null) return null;
        if (sub.getEndDate() != null && sub.getEndDate().isBefore(LocalDate.now())) {
            sub.setStatus("EXPIRED");
            subscriptionRepo.save(sub);
            return null;
        }
        return sub;
    }

    @Override
    public List<Subscription> getSubscriptionsByRecruiterIdAndStatus(int recruiterId, String status) {
        return subscriptionRepo.findByRecruiterIdAndStatus(recruiterId, status == null ? "ACTIVE" : status.trim().toUpperCase());
    }

    @Override
    public List<Subscription> getSubscriptionsByStatus(String status) {
        return subscriptionRepo.findByStatus(status == null ? "ACTIVE" : status.trim().toUpperCase());
    }

    // ─── NEW: Razorpay ───────────────────────────────────────────────────────

    @Override
    public Map<String, Object> createRazorpayOrder(int recruiterId, String plan) throws Exception {
        String normalizedPlan = plan == null ? "FREE" : plan.trim().toUpperCase();
        double amount = getPlanAmount(normalizedPlan);

        // FREE plan — no payment needed, activate directly
        if (amount == 0) {
            Subscription sub = subscribe(recruiterId, normalizedPlan);
            Map<String, Object> result = new HashMap<>();
            result.put("free", true);
            result.put("subscriptionId", sub.getSubscriptionId());
            return result;
        }

        // Check no active subscription already exists
        if (getActiveSubscription(recruiterId) != null) {
            throw new RuntimeException("Active subscription already exists");
        }

        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject options = new JSONObject();
        options.put("amount", (int)(amount * 100)); // paise
        options.put("currency", "INR");
        options.put("receipt", "rcpt_" + recruiterId + "_" + System.currentTimeMillis());

        JSONObject notes = new JSONObject();
        notes.put("plan", normalizedPlan);
        notes.put("recruiterId", recruiterId);
        options.put("notes", notes);

        Order order = client.orders.create(options);

        Map<String, Object> result = new HashMap<>();
        result.put("free", false);
        result.put("orderId", order.get("id").toString());
        result.put("amount", amount);
        result.put("currency", "INR");
        result.put("plan", normalizedPlan);
        result.put("keyId", razorpayKeyId);
        return result;
    }

    @Override
    @Transactional
    public Subscription verifyAndActivate(int recruiterId, Map<String, String> payload) throws Exception {
        String orderId   = payload.get("razorpay_order_id");
        String paymentId = payload.get("razorpay_payment_id");
        String signature = payload.get("razorpay_signature");
        String plan      = payload.get("plan");

        if (orderId == null || paymentId == null || signature == null) {
            throw new RuntimeException("Missing payment details");
        }

        // Verify Razorpay signature
        String data = orderId + "|" + paymentId;
        String generated = hmacSha256(razorpayKeySecret, data);
        if (!generated.equals(signature)) {
            throw new RuntimeException("Payment verification failed — invalid signature");
        }

        // Check no duplicate active subscription
        if (getActiveSubscription(recruiterId) != null) {
            throw new RuntimeException("Active subscription already exists");
        }

        String normalizedPlan = plan == null ? "FREE" : plan.trim().toUpperCase();
        Subscription sub = new Subscription();
        sub.setRecruiterId(recruiterId);
        sub.setPlan(normalizedPlan);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
        sub.setStatus("ACTIVE");
        sub.setAmountPaid(getPlanAmount(normalizedPlan));
        Subscription saved = subscriptionRepo.save(sub);

        generateInvoice(saved.getSubscriptionId(), saved.getAmountPaid(), recruiterId, "RAZORPAY", paymentId);
        return saved;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private double getPlanAmount(String plan) {
        if ("PROFESSIONAL".equalsIgnoreCase(plan)) return 999;
        if ("ENTERPRISE".equalsIgnoreCase(plan)) return 1999;
        return 0; // FREE
    }

    private Subscription getOwnedSubscriptionOrThrow(int recruiterId, int subscriptionId) {
        return subscriptionRepo.findBySubscriptionIdAndRecruiterId(subscriptionId, recruiterId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
