package com.tinyhis.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Cache Initializer
 * Clears Redis cache on application startup to ensure consistency with Database
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInitializer implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.redis.quota-prefix:schedule:quota:}")
    private String quotaPrefix;

    @Value("${app.redis.user-prefix:schedule:users:}")
    private String userPrefix;

    @Override
    public void run(String... args) {
        log.info("Starting cache cleanup...");

        try {
            // Clean quota cache
            Set<String> quotaKeys = redisTemplate.keys(quotaPrefix + "*");
            if (quotaKeys != null && !quotaKeys.isEmpty()) {
                redisTemplate.delete(quotaKeys);
                log.info("Cleared {} quota cache keys", quotaKeys.size());
            }

            // Clean user cache
            Set<String> userKeys = redisTemplate.keys(userPrefix + "*");
            if (userKeys != null && !userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
                log.info("Cleared {} user cache keys", userKeys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear cache on startup", e);
        }

        log.info("Cache cleanup completed.");
    }
}
