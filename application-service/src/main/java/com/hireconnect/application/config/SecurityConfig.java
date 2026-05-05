package com.hireconnect.application.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.hireconnect.application.security.JwtFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/applications/public/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/applications/job/**").hasAnyRole("RECRUITER", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/applications/candidate/**").hasRole("CANDIDATE")
            .requestMatchers(HttpMethod.GET, "/applications").hasAnyRole("RECRUITER", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/applications/*/status").hasAnyRole("RECRUITER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/applications").hasRole("CANDIDATE")
            .requestMatchers("/applications/**").authenticated()
            .anyRequest().permitAll()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
