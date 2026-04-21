package com.hireconnect.profile.service;

import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import java.util.List;
public interface ProfileService {

    CandidateProfile addCandidateProfile(CandidateProfile profile);

    RecruiterProfile addRecruiterProfile(RecruiterProfile profile);

    CandidateProfile updateCandidateProfile(Long id, CandidateProfile profile, String email);

    RecruiterProfile updateRecruiterProfile(Long id, RecruiterProfile profile,String email);

    void deleteCandidateProfile(Long id, String email);

    void deleteRecruiterProfile(Long id,String email);
    List<CandidateProfile> getAllCandidates();

    List<RecruiterProfile> getAllRecruiters();

    

    

}
