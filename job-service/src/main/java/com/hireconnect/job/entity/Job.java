package com.hireconnect.job.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long jobId;

    @Column(nullable = false)
    private String title;

    private String category;      // IT, Finance etc
    private String type;          // Full-time, Part-time

    @Column(nullable = false)
    private String location;

    private Double salaryMin;
    private Double salaryMax;

    private String skills;

    private Integer experienceRequired;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String company;

    // recruiter email from JWT
    @Column(nullable = false)
    private String postedBy;

    private String status; // OPEN, CLOSED

    private LocalDateTime postedAt;

    public Job() {}

    // Getters & Setters

    public Long getJobId() { return jobId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Double salaryMin) { this.salaryMin = salaryMin; }

    public Double getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Double salaryMax) { this.salaryMax = salaryMax; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public Integer getExperienceRequired() { return experienceRequired; }
    public void setExperienceRequired(Integer experienceRequired) { this.experienceRequired = experienceRequired; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    @PrePersist
    public void onCreate() {
        this.postedAt = LocalDateTime.now();
        this.status = "OPEN";
    }
}
