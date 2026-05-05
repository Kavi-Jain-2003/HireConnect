package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import java.util.List;
import java.util.Map;

public interface SubscriptionService {
    Subscription subscribe(int recruiterId, String plan);
    void cancelSubscription(int recruiterId, int subscriptionId);
    Subscription renewSubscription(int recruiterId, int subscriptionId);
    Invoice generateInvoice(int subscriptionId, double amount);
    List<Invoice> getInvoices(int recruiterId, int subscriptionId);
    List<Subscription> getSubscriptionsByRecruiterId(int recruiterId);
    Subscription getActiveSubscription(int recruiterId);
    List<Subscription> getSubscriptionsByRecruiterIdAndStatus(int recruiterId, String status);
    List<Subscription> getSubscriptionsByStatus(String status);

    // Razorpay
    Map<String, Object> createRazorpayOrder(int recruiterId, String plan) throws Exception;
    Subscription verifyAndActivate(int recruiterId, Map<String, String> payload) throws Exception;
}
