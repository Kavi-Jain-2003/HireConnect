package com.hireconnect.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByCandidateId(Long candidateId);

    List<Application> findByJobId(Long jobId);

    List<Application> findByStatus(ApplicationStatus status);

    Optional<Application> findFirstByJobIdAndCandidateId(Long jobId, Long candidateId);

    Long countByJobId(Long jobId);
}
