package com.hireconnect.subscription.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int subscriptionId;

    private int recruiterId;

    private String plan; // FREE, PROFESSIONAL, ENTERPRISE

    private LocalDate startDate;

    private LocalDate endDate;

    private String status; // ACTIVE, CANCELLED, EXPIRED

    private double amountPaid;

    public Subscription() {}

    public Subscription(int subscriptionId, int recruiterId, String plan,
                        LocalDate startDate, LocalDate endDate,
                        String status, double amountPaid) {
        this.subscriptionId = subscriptionId;
        this.recruiterId = recruiterId;
        this.plan = plan;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.amountPaid = amountPaid;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    // Getters and Setters

    public int getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(int subscriptionId) { this.subscriptionId = subscriptionId; }

    public int getRecruiterId() { return recruiterId; }
    public void setRecruiterId(int recruiterId) { this.recruiterId = recruiterId; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
}
