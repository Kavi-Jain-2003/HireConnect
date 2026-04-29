package com.hireconnect.subscription.controller;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.dto.ApiResponse;
import com.hireconnect.subscription.service.SubscriptionService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionResource {

    private final SubscriptionService service;

    public SubscriptionResource(SubscriptionService service) {
        this.service = service;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse> subscribe(HttpServletRequest request,
                                  @RequestParam String plan) {

        int recruiterId = getRecruiterId(request);
        Subscription saved = service.subscribe(recruiterId, plan);
        return ResponseEntity.ok(ApiResponse.of("Subscription created successfully", saved));
    }


    @PostMapping("/cancel/{id}")
    public ResponseEntity<ApiResponse> cancel(HttpServletRequest request, @PathVariable int id) {
        int recruiterId = getRecruiterId(request);
        service.cancelSubscription(recruiterId, id);
        return ResponseEntity.ok(ApiResponse.of("Subscription cancelled", null));
    }

    @PostMapping("/renew/{id}")
    public ResponseEntity<ApiResponse> renew(HttpServletRequest request, @PathVariable int id) {
        int recruiterId = getRecruiterId(request);
        Subscription saved = service.renewSubscription(recruiterId, id);
        return ResponseEntity.ok(ApiResponse.of("Subscription renewed", saved));
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<ApiResponse> getInvoices(HttpServletRequest request, @PathVariable int id) {
        int recruiterId = getRecruiterId(request);
        return ResponseEntity.ok(ApiResponse.of("Invoices fetched successfully", service.getInvoices(recruiterId, id)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse> getMySubscriptions(HttpServletRequest request) {
        int recruiterId = getRecruiterId(request);
        return ResponseEntity.ok(ApiResponse.of("Subscriptions fetched successfully", service.getSubscriptionsByRecruiterId(recruiterId)));
    }

    @GetMapping("/my/active")
    public ResponseEntity<ApiResponse> getActive(HttpServletRequest request) {
        int recruiterId = getRecruiterId(request);
        return ResponseEntity.ok(ApiResponse.of("Active subscription fetched successfully", service.getActiveSubscription(recruiterId)));
    }

    @GetMapping("/my/status/{status}")
    public ResponseEntity<ApiResponse> getByStatus(HttpServletRequest request, @PathVariable String status) {
        int recruiterId = getRecruiterId(request);
        return ResponseEntity.ok(ApiResponse.of("Subscriptions fetched successfully", service.getSubscriptionsByRecruiterIdAndStatus(recruiterId, status)));
    }

    private int getRecruiterId(HttpServletRequest request) {
        Object recruiterId = request.getAttribute("recruiterId");
        if (!(recruiterId instanceof Number number)) {
            throw new IllegalStateException("Recruiter id is missing from the request");
        }
        return number.intValue();
    }
}
