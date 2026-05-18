package com.hireconnect.auth.dto;

public class RefreshTokenRequest {

    private String token;

    public RefreshTokenRequest() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
