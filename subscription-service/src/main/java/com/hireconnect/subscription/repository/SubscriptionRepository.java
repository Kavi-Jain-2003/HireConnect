package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByRecruiterId(int recruiterId);

    List<Subscription> findByStatus(String status);

    Optional<Subscription> findBySubscriptionId(int subscriptionId);

    Optional<Subscription> findBySubscriptionIdAndRecruiterId(int subscriptionId, int recruiterId);

    Subscription findFirstByRecruiterIdAndStatus(int recruiterId, String status);

    List<Subscription> findByRecruiterIdAndStatus(int recruiterId, String status);

    int countByPlan(String plan);
}
