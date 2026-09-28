package com.civic.waterwatch.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when citizen or client request frequency exceeds the Redis sliding-window limit.
 */
@Getter
public class RateLimitExceededException extends WaterWatchException {

    private final String clientKey;
    private final int limit;
    private final int windowSeconds;
    private final int retryAfterSeconds;

    public RateLimitExceededException(String clientKey, int limit, int windowSeconds) {
        super(
                String.format("Rate limit exceeded for client '%s'. Allowed: %d requests per %d seconds.", clientKey, limit, windowSeconds),
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED",
                Map.of(
                        "clientKey", clientKey != null ? clientKey : "unknown",
                        "limit", limit,
                        "windowSeconds", windowSeconds,
                        "retryAfterSeconds", windowSeconds
                )
        );
        this.clientKey = clientKey;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        this.retryAfterSeconds = windowSeconds;
    }
}
