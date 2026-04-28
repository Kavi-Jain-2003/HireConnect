package com.hireconnect.auth.security;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.entity.AuthProvider;
import com.hireconnect.auth.entity.Role;
import com.hireconnect.auth.repository.AuthRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final AuthRepository authRepository;

    public OAuth2SuccessHandler(JwtUtil jwtUtil, AuthRepository authRepository) {
        this.jwtUtil = jwtUtil;
        this.authRepository = authRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String email = user.getAttribute("email");

        // 🔥 If email is null (GitHub issue)
        if (email == null) {
            email = user.getAttribute("login") + "@github.com";
        }

        // ✅ Save user if not exists
        UserCredential existingUser = authRepository.findByEmail(email).orElse(null);

        if (existingUser == null) {
            UserCredential newUser = new UserCredential();
            newUser.setEmail(email);
            newUser.setPasswordHash("OAUTH_USER");
            newUser.setRole(Role.CANDIDATE); // default
            newUser.setProvider(AuthProvider.GITHUB);
            newUser.setCreatedAt(LocalDateTime.now());

            existingUser = authRepository.save(newUser);
        }

        // 🔐 Generate JWT
        String token = jwtUtil.generateToken(email, "CANDIDATE", existingUser.getUserId());

        // 👉 Return token in response
        response.getWriter().write("JWT Token: " + token);
    }
}
