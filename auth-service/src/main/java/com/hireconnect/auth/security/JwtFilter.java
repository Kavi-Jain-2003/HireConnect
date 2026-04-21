package com.hireconnect.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	public JwtFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// reads header from request
		String authHeader = request.getHeader("Authorization");

		// 🔴 No token → no continue, use is not authenticated
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			SecurityContextHolder.clearContext();
			// sprint gives 401 as user is not authenticated
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);

		try {
			// verifying token with signature
			String email = jwtUtil.extractEmail(token);

			// prevents duplicate authentication
			if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, null,
						Collections.emptyList() // roles later
				);

				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				// user is logged in
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}

		} catch (JwtException e) {
			SecurityContextHolder.clearContext(); // 🔥 clear invalid auth 401
		}

		filterChain.doFilter(request, response);
	}
}
