package com.hireconnect.profile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.profile.entity.*;
import com.hireconnect.profile.dto.ApiResponse;
import com.hireconnect.profile.service.ProfileService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/profiles")
public class ProfileResource {

    @Autowired
    private ProfileService profileService;

    @PostMapping("/candidate")
    public ResponseEntity<ApiResponse> addCandidate(@RequestBody CandidateProfile profile,
                               HttpServletRequest request) {

        String email = (String) request.getAttribute("email");
        Long userId = (Long) request.getAttribute("userId");

        profile.setEmail(email);
        profile.setUserId(userId);

        profileService.addCandidateProfile(profile);

        return ResponseEntity.ok(ApiResponse.of("Candidate profile created successfully", null));
    }


    @PostMapping("/recruiter")
    public ResponseEntity<ApiResponse> addRecruiter(@RequestBody RecruiterProfile profile,
                               HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profile.setEmail(email);

        profileService.addRecruiterProfile(profile);

        return ResponseEntity.ok(ApiResponse.of("Recruiter profile created successfully", null));
    }

    @GetMapping("/public/candidate/email")
    public ResponseEntity<ApiResponse> getCandidateByEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.of("Candidate profile fetched successfully", profileService.getCandidateByEmail(email)));
    }

    @GetMapping("/public/candidate/id/{id}")
    public ResponseEntity<ApiResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Candidate profile fetched successfully", profileService.getCandidateById(id)));
    }

    @GetMapping("/public/recruiter/email")
    public ResponseEntity<ApiResponse> getRecruiterByEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.of("Recruiter profile fetched successfully", profileService.getRecruiterByEmail(email)));
    }

    @GetMapping("/public/recruiter/id/{id}")
    public ResponseEntity<ApiResponse> getRecruiterById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Recruiter profile fetched successfully", profileService.getRecruiterById(id)));
    }


    @PutMapping("/candidate/{id}")
    public ResponseEntity<ApiResponse> updateCandidate(@PathVariable Long id,
                                  @RequestBody CandidateProfile profile,
                                  HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profileService.updateCandidateProfile(id, profile, email);

        return ResponseEntity.ok(ApiResponse.of("Candidate updated successfully", null));
    }


    @PutMapping("/recruiter/{id}")
    public ResponseEntity<ApiResponse> updateRecruiter(@PathVariable Long id,
                                            @RequestBody RecruiterProfile profile, HttpServletRequest request) {
    	 String email = (String) request.getAttribute("email");

    	    profileService.updateRecruiterProfile(id, profile, email);

    	    return ResponseEntity.ok(ApiResponse.of("Recruiter updated successfully", null));
    }

    @DeleteMapping("/candidate/{id}")
    public ResponseEntity<ApiResponse> deleteCandidate(@PathVariable Long id,
                                  HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profileService.deleteCandidateProfile(id, email);

        return ResponseEntity.ok(ApiResponse.of("Candidate deleted successfully", null));
    }

    @DeleteMapping("/recruiter/{id}")
    public ResponseEntity<ApiResponse> deleteRecruiter(@PathVariable Long id,HttpServletRequest request) {
    	String email = (String) request.getAttribute("email");

        profileService.deleteRecruiterProfile(id, email);

        return ResponseEntity.ok(ApiResponse.of("Recruiter deleted successfully", null));
    }
    @GetMapping("/public/candidates")
    public ResponseEntity<ApiResponse> getAllCandidates() {
        return ResponseEntity.ok(ApiResponse.of("Candidates fetched successfully", profileService.getAllCandidates()));
    }

    @GetMapping("/public/recruiters")
    public ResponseEntity<ApiResponse> getAllRecruiters() {
        return ResponseEntity.ok(ApiResponse.of("Recruiters fetched successfully", profileService.getAllRecruiters()));
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN ONLY — delete any profile without ownership check
    // ─────────────────────────────────────────────────────────────

    @DeleteMapping("/admin/candidate/{id}")
    public ResponseEntity<ApiResponse> adminDeleteCandidate(@PathVariable Long id,
                                                            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(ApiResponse.of("Access denied. Admins only.", null));
        }
        // Pass a dummy "admin" email — service will skip ownership check if we
        // expose a new method, but to keep it simple we reuse deleteCandidateProfile
        // after fetching the profile's own email.
        CandidateProfile profile = profileService.getCandidateById(id);
        if (profile == null) {
            return ResponseEntity.status(404).body(ApiResponse.of("Candidate not found", null));
        }
        profileService.deleteCandidateProfile(id, profile.getEmail());
        return ResponseEntity.ok(ApiResponse.of("Candidate profile deleted by admin", null));
    }

    @DeleteMapping("/admin/recruiter/{id}")
    public ResponseEntity<ApiResponse> adminDeleteRecruiter(@PathVariable Long id,
                                                            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(ApiResponse.of("Access denied. Admins only.", null));
        }
        RecruiterProfile profile = profileService.getRecruiterById(id);
        if (profile == null) {
            return ResponseEntity.status(404).body(ApiResponse.of("Recruiter not found", null));
        }
        profileService.deleteRecruiterProfile(id, profile.getEmail());
        return ResponseEntity.ok(ApiResponse.of("Recruiter profile deleted by admin", null));
    }
}
