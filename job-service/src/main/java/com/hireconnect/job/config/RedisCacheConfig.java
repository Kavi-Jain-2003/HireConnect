package com.hireconnect.job.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for job-service.
 *
 * Cache names and TTLs:
 *  "jobs"         — all jobs list    — TTL 5 min  (refreshed on any write)
 *  "job"          — single job by id — TTL 10 min
 *  "jobsByCategory" / "jobsByLocation" — TTL 5 min
 *
 * How to use in JobServiceImpl:
 *   @Cacheable("jobs")              on getAllJobs()
 *   @Cacheable(value="job", key="#id")  on getJobById()
 *   @CacheEvict(allEntries=true, value={"jobs","jobsByCategory","jobsByLocation"})
 *                                   on addJob(), updateJob(), deleteJob()
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // Default config for unlisted caches
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("jobs",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put("job",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        cacheConfigs.put("jobsByCategory",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put("jobsByLocation",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
