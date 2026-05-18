package com.hireconnect.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hireconnect.profile.entity.RecruiterProfile;
import java.util.Optional;
import java.util.List;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {

    Optional<RecruiterProfile> findByEmail(String email);
    List<RecruiterProfile> findAllByRole(String role);
    List<RecruiterProfile> findAll();

}
