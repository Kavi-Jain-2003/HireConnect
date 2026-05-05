package com.hireconnect.auth.config;

import com.hireconnect.auth.security.JwtFilter;
import com.hireconnect.auth.security.OAuth2SuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter,OAuth2SuccessHandler oAuth2SuccessHandler) {
    	this.jwtFilter = jwtFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	http
        .csrf(csrf -> csrf.disable())

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/admin/**").permitAll()  // role-check done inside AdminController
            .anyRequest().authenticated()
        )

        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"message\": \"Please login first\"}");
            })
        )

        .oauth2Login(oauth -> oauth
            .successHandler(oAuth2SuccessHandler)
        )

        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        return http.build();
    }
}
//http://localhost:8081/oauth2/authorization/github
