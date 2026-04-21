package com.hireconnect.job.config;

import com.hireconnect.job.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // ✅ PUBLIC APIs (NO TOKEN REQUIRED)
                .requestMatchers("/jobs/public/**").permitAll()

                // 🔐 ONLY RECRUITER CAN CREATE/UPDATE/DELETE JOB
                .requestMatchers(HttpMethod.POST, "/jobs").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.PUT, "/jobs/**").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.DELETE, "/jobs/**").hasRole("RECRUITER")

                // 🔐 ALL OTHER APIs REQUIRE LOGIN
                .anyRequest().authenticated()
            )

            // ❌ No session (JWT based)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
            	    .authenticationEntryPoint((request, response, authException) -> {
            	        response.setContentType("application/json");
            	        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            	        response.getWriter().write("{\"message\": \"Please login first\"}");
            	    })
            	    .accessDeniedHandler((request, response, ex2) -> {
            	        response.setContentType("application/json");
            	        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            	        response.getWriter().write("{\"message\": \"Only recruiter can perform this action\"}");
            	    })
    )

            // 🔥 JWT FILTER
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
