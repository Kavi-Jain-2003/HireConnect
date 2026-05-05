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
                // PUBLIC — no token required
                .requestMatchers("/jobs/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/jobs/**").permitAll()   // ← covers /jobs/3, /jobs/2 etc.

                // RECRUITER ONLY — create / update / delete
                .requestMatchers(HttpMethod.POST,   "/jobs").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.PUT,    "/jobs/**").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.DELETE, "/jobs/**").hasRole("RECRUITER")

                .anyRequest().authenticated()
            )

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

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}