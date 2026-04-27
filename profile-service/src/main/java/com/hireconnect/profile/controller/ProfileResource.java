package com.hireconnect.profile.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.profile.entity.*;
import com.hireconnect.profile.service.ProfileService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/profiles")
public class ProfileResource {

    @Autowired
    private ProfileService profileService;

    @PostMapping("/candidate")
    public String addCandidate(@RequestBody CandidateProfile profile,
                               HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profile.setEmail(email);

        profileService.addCandidateProfile(profile);

        return "Candidate profile created successfully";
    }


    @PostMapping("/recruiter")
    public String addRecruiter(@RequestBody RecruiterProfile profile,
                               HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profile.setEmail(email);

        profileService.addRecruiterProfile(profile);

        return "Recruiter profile created successfully";
    }

    @GetMapping("/public/candidate/email/{email}")
    public CandidateProfile getCandidateByEmail(@PathVariable String email) {
        return profileService.getCandidateByEmail(email);
    }

    @GetMapping("/public/candidate/id/{id}")
    public CandidateProfile getCandidateById(@PathVariable Long id) {
        return profileService.getCandidateById(id);
    }

    @GetMapping("/public/recruiter/email/{email}")
    public RecruiterProfile getRecruiterByEmail(@PathVariable String email) {
        return profileService.getRecruiterByEmail(email);
    }

    @GetMapping("/public/recruiter/id/{id}")
    public RecruiterProfile getRecruiterById(@PathVariable Long id) {
        return profileService.getRecruiterById(id);
    }


    @PutMapping("/candidate/{id}")
    public String updateCandidate(@PathVariable Long id,
                                  @RequestBody CandidateProfile profile,
                                  HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profileService.updateCandidateProfile(id, profile, email);

        return "Candidate updated successfully";
    }


    @PutMapping("/recruiter/{id}")
    public String updateRecruiter(@PathVariable Long id,
                                            @RequestBody RecruiterProfile profile, HttpServletRequest request) {
    	 String email = (String) request.getAttribute("email");

    	    profileService.updateRecruiterProfile(id, profile, email);

    	    return "Recruiter updated successfully";
    }

    @DeleteMapping("/candidate/{id}")
    public String deleteCandidate(@PathVariable Long id,
                                  HttpServletRequest request) {

        String email = (String) request.getAttribute("email");

        profileService.deleteCandidateProfile(id, email);

        return "Candidate deleted successfully";
    }

    @DeleteMapping("/recruiter/{id}")
    public String deleteRecruiter(@PathVariable Long id,HttpServletRequest request) {
    	String email = (String) request.getAttribute("email");

        profileService.deleteRecruiterProfile(id, email);

  
        return "Recruiter deleted successfully";
    }
    @GetMapping("/public/candidates")
    public List<CandidateProfile> getAllCandidates() {
        return profileService.getAllCandidates();
    }

    @GetMapping("/public/recruiters")
    public List<RecruiterProfile> getAllRecruiters() {
        return profileService.getAllRecruiters();
    }

}
