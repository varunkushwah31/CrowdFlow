package com.civic.waterwatch.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Distributed Rate Limiter powered by Redis atomic operations.
 * Protects citizen intake endpoints (/api/reports, /api/auth) from spam and DDoS.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_PREFIX = "crowdflow:ratelimit:";

    /**
     * Checks if a request from the given key (e.g. IP address or phone) is within limits.
     *
     * @param clientKey       Unique identifier (e.g. IP address or citizen phone)
     * @param maxRequests     Maximum allowed requests in the time window
     * @param windowSeconds   Window duration in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String clientKey, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + clientKey;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                // First request in this window -> initialize expiration
                stringRedisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key '{}': {}/{} requests in {}s",
                        clientKey, count, maxRequests, windowSeconds);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Redis rate limiter error for '{}', allowing request as fail-open: {}", clientKey, e.getMessage());
            return true; // Fail-open to avoid service disruption if Redis is temporarily unreachable
        }
    }

    /**
     * Returns remaining requests allowed in current window.
     */
    public long getRemainingRequests(String clientKey, int maxRequests) {
        String redisKey = RATE_LIMIT_PREFIX + clientKey;
        try {
            String val = stringRedisTemplate.opsForValue().get(redisKey);
            if (val == null) {
                return maxRequests;
            }
            long current = Long.parseLong(val);
            return Math.max(0, maxRequests - current);
        } catch (Exception e) {
            return maxRequests;
        }
    }

    /**
     * Returns time-to-live of the rate limit window in seconds.
     */
    public long getTimeToResetSeconds(String clientKey) {
        String redisKey = RATE_LIMIT_PREFIX + clientKey;
        try {
            Long expire = stringRedisTemplate.getExpire(redisKey);
            return expire != null && expire > 0 ? expire : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
