package com.hireconnect.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.job.entity.Job;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByTitleContainingIgnoreCase(String title);

    List<Job> findByCategory(String category);

    List<Job> findByLocation(String location);

    List<Job> findByPostedBy(String postedBy);

    List<Job> findByStatus(String status);
}
