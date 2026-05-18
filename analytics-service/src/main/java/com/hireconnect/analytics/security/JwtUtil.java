package com.hireconnect.analytics.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    // Bug 8 fix: inject from application.properties
    @Value("${jwt.secret}")
    private String secretKey;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().get("role", String.class);
    }

    public Long extractUserId(String token) {
        Object userId = Jwts.parserBuilder()
                .setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().get("userId");

        if (userId == null) return null;
        if (userId instanceof Number number) return number.longValue();
        return Long.valueOf(userId.toString());
    }
}
