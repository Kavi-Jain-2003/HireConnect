package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.LoginResponse;
import com.hireconnect.auth.dto.RefreshTokenRequest;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.service.AuthService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

//    public String login(@RequestBody LoginRequest request)
//    {
//    	return authService.login(request);
//    }
    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        return "Logged out successfully";

    }

    @PostMapping("/validate")
    public TokenValidationResponse validate(@RequestBody RefreshTokenRequest request) {
        return authService.validateToken(request.getToken());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getToken());
    }
}
@RestController
@RequestMapping("/api")
 class TestController {

    @GetMapping("/test")
    public String test() {
        return "Protected API working!";
    }
}
