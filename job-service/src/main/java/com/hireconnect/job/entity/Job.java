package com.hireconnect.job.entity;

import java.time.LocalDateTime;
import java.util.List;

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

    private String category; // IT, Finance etc
    private String type;     // Full-time, Part-time

    @Column(nullable = false)
    private String location;

    private Double salaryMin;
    private Double salaryMax;

    // Changed from String to List<String> so each skill is a separate row,
    // enabling proper filtering and querying per skill.
    @ElementCollection
    @CollectionTable(
        name = "job_skills",
        joinColumns = @JoinColumn(name = "job_id"),
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @Column(name = "skill")
    private List<String> skills;

    private Integer experienceRequired;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String company;

    // recruiter email from JWT
    @Column(nullable = false)
    private String postedBy;

    private String status; // OPEN, PAUSED, CLOSED

    @Column(name = "created_at")
    private LocalDateTime postedAt;

    // Fix 7: track how many times this job listing has been viewed
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer viewCount = 0;

    public Job() {}

    // Getters & Setters

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

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

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

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

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    @PrePersist
    public void onCreate() {
        this.postedAt = LocalDateTime.now();
        this.status = "OPEN";
        if (this.viewCount == null) this.viewCount = 0;
    }
}