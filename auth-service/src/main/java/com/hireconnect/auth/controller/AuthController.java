package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.ApiResponse;
import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.LoginResponse;
import com.hireconnect.auth.dto.RefreshTokenRequest;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRepository authRepository;

    public AuthController(AuthService authService, AuthRepository authRepository) {
        this.authService = authService;
        this.authRepository = authRepository;
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

    @GetMapping("/public/users/email/{email}")
    public ResponseEntity<ApiResponse> getUserByEmail(@PathVariable String email) {
        UserCredential user = authRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(ApiResponse.of("User fetched successfully", java.util.Map.of(
                "userId", user.getUserId(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        )));
    }
}
