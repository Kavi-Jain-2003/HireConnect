package com.hireconnect.profile.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hireconnect.profile.entity.*;
import com.hireconnect.profile.repository.*;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private CandidateProfileRepository candidateRepo;

    @Autowired
    private RecruiterProfileRepository recruiterRepo;

    @Override
    public CandidateProfile addCandidateProfile(CandidateProfile profile) {
        return candidateRepo.save(profile);
    }

    @Override
    public RecruiterProfile addRecruiterProfile(RecruiterProfile profile) {
        return recruiterRepo.save(profile);
    }

    @Override
    public CandidateProfile updateCandidateProfile(Long id, CandidateProfile profile, String email) {

        CandidateProfile existing = candidateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (!existing.getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access");
        }

        profile.setProfileId(existing.getProfileId());
        profile.setEmail(existing.getEmail());
        profile.setUserId(existing.getUserId());

        return candidateRepo.save(profile);
    }

    @Override
    public RecruiterProfile updateRecruiterProfile(Long id, RecruiterProfile profile, String email) {
        RecruiterProfile existing = recruiterRepo.findById(id).orElseThrow(()->new RuntimeException("recruiter not found"));
        if (!existing.getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access");
        }
        profile.setProfileId(existing.getProfileId());
        profile.setEmail(existing.getEmail());
        return recruiterRepo.save(profile);
    }

    @Override
    public CandidateProfile getCandidateByEmail(String email) {
        return candidateRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
    }

    @Override
    public CandidateProfile getCandidateById(Long id) {
        return candidateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
    }

    @Override
    public RecruiterProfile getRecruiterByEmail(String email) {
        return recruiterRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
    }

    @Override
    public RecruiterProfile getRecruiterById(Long id) {
        return recruiterRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
    }

    @Override
    public void deleteCandidateProfile(Long id, String email) {

        CandidateProfile existing = candidateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (!existing.getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access");
        }

        candidateRepo.delete(existing);
    }

    @Override
    public void deleteRecruiterProfile(Long id,String email) {
    	RecruiterProfile existing = recruiterRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("recruiter not found"));

        if (!existing.getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access");
        }

        recruiterRepo.delete(existing);
      
    }
    @Override
    public List<CandidateProfile> getAllCandidates() {
        return candidateRepo.findAll();
    }

    @Override
    public List<RecruiterProfile> getAllRecruiters() {
        return recruiterRepo.findAll();
    }

}
