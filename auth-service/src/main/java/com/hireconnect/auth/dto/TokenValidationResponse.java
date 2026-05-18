
package com.hireconnect.auth.dto;

public class TokenValidationResponse {

    private boolean valid;
    private String email;

    public TokenValidationResponse(boolean valid, String email) {
        this.valid = valid;
        this.email = email;
    }

    public boolean isValid() {
        return valid;
    }

    public String getEmail() {
        return email;
    }
}
