package com.hireconnect.application.dto;

public class ApplicationResponse {

    private String message;
    private Long applicationId;

    public ApplicationResponse() {
    }

    public ApplicationResponse(String message, Long applicationId) {
        this.message = message;
        this.applicationId = applicationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }
}
