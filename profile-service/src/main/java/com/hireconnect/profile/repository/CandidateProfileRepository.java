package com.hireconnect.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hireconnect.profile.entity.CandidateProfile;
import java.util.Optional;
import java.util.List;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByEmail(String email);
    Optional<CandidateProfile> findByMobile(String mobile);
    List<CandidateProfile> findAllByRole(String role);

    List<CandidateProfile> findAll();

}
