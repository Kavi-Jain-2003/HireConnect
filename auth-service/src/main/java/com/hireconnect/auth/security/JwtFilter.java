package com.hireconnect.auth.security;

import com.hireconnect.auth.service.TokenBlackListService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Updated JwtFilter — adds Redis blacklist check after signature validation.
 *
 * Changes from original:
 *  1. Injects TokenBlackListService.
 *  2. After extracting the token, checks isBlacklisted() — rejects with 401 if found.
 *  3. Passes role as a GrantedAuthority so @PreAuthorize works correctly.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlackListService blacklistService;

    public JwtFilter(JwtUtil jwtUtil, TokenBlackListService blacklistService) {
        this.jwtUtil          = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.equals("/swagger-ui.html")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {
String path = request.getRequestURI();
    if (path.startsWith("/admin")) {
        String role = request.getHeader("X-User-Role");
        request.setAttribute("role", role);
        filterChain.doFilter(request, response);
        return;  // ← skip JWT re-validation for admin routes
    }
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // ── 1. Validate signature & expiry ─────────────────────────────
            String email = jwtUtil.extractEmail(token);
            String role  = jwtUtil.extractRole(token);

            // ── 2. Redis blacklist check (logout invalidation) ─────────────
            if (blacklistService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Token has been invalidated. Please log in again.\"}");
                return;
            }

            // ── 3. Set authentication context ──────────────────────────────
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities =
                        role != null ? List.of(new SimpleGrantedAuthority(role)) : Collections.emptyList();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            request.setAttribute("email", email);
            request.setAttribute("role", role);

        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
