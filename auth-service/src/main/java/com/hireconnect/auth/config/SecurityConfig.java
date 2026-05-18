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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtFilter jwtFilter;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(JwtFilter jwtFilter, OAuth2SuccessHandler oAuth2SuccessHandler, ClientRegistrationRepository clientRegistrationRepository) {
        this.jwtFilter = jwtFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/admin/**").permitAll()
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
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(authorizationRequestResolver(this.clientRegistrationRepository))
                )
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
            new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer ->
            customizer.additionalParameters(params -> params.put("prompt", "select_account"))
        );
        return resolver;
    }
}
