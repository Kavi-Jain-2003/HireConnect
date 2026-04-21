package com.hireconnect.auth.service;

import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.LoginResponse;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;

public interface AuthService {

    String register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    String logout(String token);

    TokenValidationResponse validateToken(String token);

    LoginResponse refreshToken(String token);
}
