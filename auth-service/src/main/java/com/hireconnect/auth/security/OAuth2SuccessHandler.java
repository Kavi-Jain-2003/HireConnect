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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        // GitHub sometimes doesn't expose email — fall back to login@github.com
        if (email == null) {
            email = user.getAttribute("login") + "@github.com";
        }

        // Save user if not exists
        UserCredential existingUser = authRepository.findByEmail(email).orElse(null);

        if (existingUser == null) {
            UserCredential newUser = new UserCredential();
            newUser.setEmail(email);
            newUser.setPasswordHash("OAUTH_USER");
            newUser.setRole(Role.CANDIDATE);
            newUser.setProvider(AuthProvider.GITHUB);
            newUser.setCreatedAt(LocalDateTime.now());
            existingUser = authRepository.save(newUser);
        }

        // Generate JWT with correct role
        String role = existingUser.getRole().name();
        String token = jwtUtil.generateToken(email, role, existingUser.getUserId());

        // ✅ Redirect to /github-callback (NOT /auth/login) so Angular proxy
        //    does NOT intercept it and forward it to the backend.
        //    GithubCallbackComponent reads ?token=&email=&role= and stores them.
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String redirectUrl = "http://localhost:4200/auth/github/callback"
                + "?token=" + token
                + "&email=" + encodedEmail
                + "&role=" + role;

        response.sendRedirect(redirectUrl);
    }
}