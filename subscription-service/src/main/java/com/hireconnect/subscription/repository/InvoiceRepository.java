package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    List<Invoice> findBySubscriptionId(int subscriptionId);

    List<Invoice> findByRecruiterId(int recruiterId);

    Optional<Invoice> findFirstBySubscriptionIdOrderByPaymentDateDesc(int subscriptionId);
}
