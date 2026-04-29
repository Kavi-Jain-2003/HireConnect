package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.hireconnect.subscription.repository.InvoiceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final InvoiceRepository invoiceRepo;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepo,
                                   InvoiceRepository invoiceRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
    }

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

        generateInvoice(saved.getSubscriptionId(), saved.getAmountPaid(), saved.getRecruiterId());

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

        generateInvoice(saved.getSubscriptionId(), saved.getAmountPaid(), saved.getRecruiterId());

        return saved;
    }

    @Override
    public Invoice generateInvoice(int subscriptionId, double amount) {
        return generateInvoice(subscriptionId, amount, 0);
    }

    private Invoice generateInvoice(int subscriptionId, double amount, int recruiterId) {

        Invoice invoice = new Invoice();
        invoice.setSubscriptionId(subscriptionId);
        invoice.setRecruiterId(recruiterId);
        invoice.setAmount(amount);
        invoice.setPaymentDate(LocalDateTime.now());
        invoice.setPaymentMode("ONLINE");
        invoice.setTransactionId(UUID.randomUUID().toString());

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
        if (sub == null) {
            return null;
        }

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

    private double getPlanAmount(String plan) {
        if ("PROFESSIONAL".equalsIgnoreCase(plan)) return 999;
        if ("ENTERPRISE".equalsIgnoreCase(plan)) return 1999;
        return 0; // FREE
    }

    private Subscription getOwnedSubscriptionOrThrow(int recruiterId, int subscriptionId) {
        return subscriptionRepo.findBySubscriptionIdAndRecruiterId(subscriptionId, recruiterId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
    }
}
