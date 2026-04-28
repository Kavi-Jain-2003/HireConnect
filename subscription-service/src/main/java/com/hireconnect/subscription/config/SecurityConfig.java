package com.hireconnect.subscription.config;

import com.hireconnect.subscription.security.JwtFilter;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/subscriptions/subscribe").hasRole("RECRUITER")
                    .requestMatchers(HttpMethod.POST, "/subscriptions/cancel/**").hasRole("RECRUITER")
                    .requestMatchers(HttpMethod.POST, "/subscriptions/renew/**").hasRole("RECRUITER")
                    .requestMatchers(HttpMethod.GET, "/subscriptions/**").hasRole("RECRUITER")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
