package com.hireconnect.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtFilter implements GlobalFilter, Ordered {

    private static final String SECRET_KEY = "mysecretkeymysecretkeymysecretkey123456";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }

        try {
            Claims claims = parseClaims(authHeader.substring(7));
            String role = normalizeRole(claims.get("role", String.class));
            String email = claims.getSubject();
            Long userId = extractUserId(claims);

            String method = exchange.getRequest().getMethod() == null
                    ? ""
                    : exchange.getRequest().getMethod().name();

            if (!isAllowed(method, path, role)) {
                return forbidden(exchange, "Access denied");
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Email", email == null ? "" : email)
                    .header("X-User-Role", role)
                    .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception ex) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/swagger-ui.html")
                || path.contains("/v3/api-docs")
                || path.startsWith("/swagger-resources/")
                || path.startsWith("/webjars/")
                || path.startsWith("/jobs/public/")
                || path.equals("/jobs/public")
                || path.matches("/jobs/\\d+")
                || path.startsWith("/profiles/public/")
                || path.startsWith("/applications/public/")
                || path.startsWith("/notifications/public/")
                || path.startsWith("/actuator/");
    }

    private boolean isAllowed(String method, String path, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }

        if (path.startsWith("/jobs/")) {
            if (List.of("POST", "PUT", "DELETE").contains(method)) {
                return "RECRUITER".equals(role);
            }
            return true;
        }

        if (path.startsWith("/profiles/")) {
            if (path.startsWith("/profiles/candidate/")) {
                return "CANDIDATE".equals(role);
            }
            if (path.startsWith("/profiles/recruiter/")) {
                return "RECRUITER".equals(role);
            }
            return true;
        }

        if (path.startsWith("/applications/")) {
            if (path.startsWith("/applications/job/")) {
                return "RECRUITER".equals(role);
            }
            if (path.contains("/status")) {
                return "RECRUITER".equals(role);
            }
            if ("POST".equals(method) || "DELETE".equals(method)) {
                return "CANDIDATE".equals(role);
            }
            if (path.startsWith("/applications/candidate/")) {
                return "CANDIDATE".equals(role);
            }
            return true;
        }

        if (path.startsWith("/interviews/")) {
            if ("POST".equals(method) || path.endsWith("/cancel")) {
                return "RECRUITER".equals(role);
            }
            if (path.endsWith("/confirm") || path.endsWith("/reschedule")) {
                return "CANDIDATE".equals(role);
            }
            return true;
        }

        if (path.startsWith("/subscriptions/")) {
            return "RECRUITER".equals(role);
        }

        if (path.startsWith("/notifications/")) {
            return true;
        }

        if (path.startsWith("/analytics/")) {
            if (path.startsWith("/analytics/admin")) {
                return "ADMIN".equals(role);
            }
            if (path.startsWith("/analytics/recruiter/")) {
                return "RECRUITER".equals(role);
            }
            return true;
        }

        return true;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        if ("USER".equalsIgnoreCase(role)) {
            return "CANDIDATE";
        }
        return role.toUpperCase();
    }

    private Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId == null) {
            return null;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(userId.toString());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeResponse(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeResponse(exchange, HttpStatus.FORBIDDEN, message);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
