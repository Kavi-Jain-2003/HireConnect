package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.entity.Invoice;
import java.util.List;

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
}
