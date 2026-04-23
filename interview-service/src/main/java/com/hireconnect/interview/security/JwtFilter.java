package com.hireconnect.interview.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class JwtFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("Missing or Invalid Authorization Header");
            return;
        }

        try {
            String token = header.substring(7);

            String email = JwtUtil.extractEmail(token);
            String role = JwtUtil.extractRole(token);

            req.setAttribute("email", email);
            req.setAttribute("role", role);

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\": \"Invalid or Expired Token\"}");
            return;
        }


        chain.doFilter(request, response);
    }
}
