package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.ApiResponse;
import com.hireconnect.auth.dto.LoginResponse;
import com.hireconnect.auth.dto.RefreshTokenRequest;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.register(request), null));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of("Login successful", response));
    }

//    public String login(@RequestBody LoginRequest request)
//    {
//    	return authService.login(request);
//    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String header) {
        return ResponseEntity.ok(ApiResponse.of("Logged out successfully", null));

    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse> validate(@RequestBody RefreshTokenRequest request) {
        TokenValidationResponse response = authService.validateToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.of("Token validation result", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.of("Token refreshed", response));
    }
}
@RestController
@RequestMapping("/api")
 class TestController {

    @GetMapping("/test")
    public ApiResponse test() {
        return ApiResponse.of("Protected API working!", null);
    }
}
