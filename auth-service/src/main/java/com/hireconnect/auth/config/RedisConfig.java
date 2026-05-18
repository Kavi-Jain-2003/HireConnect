package com.hireconnect.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for auth-service.
 *
 * Used for:
 *  1. JWT token blacklist — invalidated tokens are stored here on logout
 *     so they cannot be reused until they naturally expire.
 *  2. (Optional) Login attempt rate-limiting per IP / email.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate<String, String>
     *
     * key   : "blacklist:<jwtToken>"
     * value : "1"
     * TTL   : remaining JWT lifetime (so Redis auto-expires the entry)
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // Store both key and value as plain strings
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
