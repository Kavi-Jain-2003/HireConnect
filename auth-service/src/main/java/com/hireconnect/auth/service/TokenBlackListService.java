package com.hireconnect.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT token blacklist backed by Redis.
 *
 * When a user logs out, their token is added here with a TTL equal to the
 * token's remaining valid lifetime. The JwtFilter checks this store on every
 * request and rejects blacklisted tokens even if the JWT signature is valid.
 *
 * Falls back gracefully (logs a warning) if Redis is unavailable, so the
 * service still works in dev without a running Redis instance — logout simply
 * won't blacklist the token in that case.
 */
@Service
public class TokenBlackListService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlackListService.class);
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final ObjectProvider<RedisTemplate<String, String>> redisProvider;

    public TokenBlackListService(ObjectProvider<RedisTemplate<String, String>> redisProvider) {
        this.redisProvider = redisProvider;
    }

    /**
     * Add a token to the blacklist.
     *
     * @param token     raw JWT string (without "Bearer " prefix)
     * @param ttl       how long the token would remain valid — Redis entry expires after this
     */
    public void blacklist(String token, Duration ttl) {
        if (token == null || token.isBlank()) return;

        RedisTemplate<String, String> redis = redisProvider.getIfAvailable();
        if (redis == null) {
            log.warn("Redis unavailable — token NOT blacklisted. Logout token: {}...{}",
                    token.substring(0, Math.min(10, token.length())), "***");
            return;
        }
        try {
            redis.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttl);
            log.info("Token blacklisted (TTL={})", ttl);
        } catch (Exception ex) {
            log.error("Failed to blacklist token in Redis: {}", ex.getMessage());
        }
    }

    /**
     * Check whether a token has been blacklisted (i.e. the user logged out).
     *
     * @param token raw JWT string
     * @return true if blacklisted and should be rejected
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;

        RedisTemplate<String, String> redis = redisProvider.getIfAvailable();
        if (redis == null) return false; // Redis down → allow (degraded gracefully)

        try {
            return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception ex) {
            log.error("Redis check failed for blacklist — allowing token: {}", ex.getMessage());
            return false;
        }
    }
}
