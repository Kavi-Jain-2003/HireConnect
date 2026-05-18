package com.hireconnect.job.dto;

public class JobResponse {
    private Long id;
    private String title;
    private String company;

    public JobResponse() {}  // public so Jackson can deserialize

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
}
