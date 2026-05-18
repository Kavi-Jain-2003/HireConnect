package com.hireconnect.analytics.dto;

public class JobDTO {

    private Long jobId;
    private String title;
    private String category;
    private String postedBy;
    private String status;
    private Integer viewCount;

    public JobDTO() {}

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
}
