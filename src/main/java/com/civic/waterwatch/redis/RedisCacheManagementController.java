package com.civic.waterwatch.redis;

import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller exposing Redis Cache Management, Native Geospatial Indexing,
 * Distributed Rate Limiting, and Citizen OTP Verification operations for India.
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Redis Operations & Cache Management (India)",
     description = "Endpoints for checking Redis cluster status, flushing cache partitions, sub-millisecond geo queries, and citizen mobile OTP verification")
public class RedisCacheManagementController {

    private final CacheManager cacheManager;
    private final RedisGeoSpatialService redisGeoSpatialService;
    private final RedisOtpService redisOtpService;
    private final RedisRateLimiterService rateLimiterService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final WaterReportRepository reportRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get Redis cluster health, cache partition stats, and geospatial indexing metrics")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 1. Connectivity Ping
        String pingResult = "UNAVAILABLE";
        Properties memoryProps = new Properties();
        Properties serverProps = new Properties();
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            pingResult = conn.ping();
            Properties info = conn.serverCommands().info();
            if (info != null) {
                info.forEach((k, v) -> {
                    String keyStr = k.toString();
                    if (keyStr.startsWith("used_memory") || keyStr.equals("total_system_memory_human")) {
                        memoryProps.put(k, v);
                    }
                    if (keyStr.equals("redis_version") || keyStr.equals("uptime_in_days") || keyStr.equals("connected_clients")) {
                        serverProps.put(k, v);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Redis ping failed: {}", e.getMessage());
        }
        stats.put("status", "PONG".equalsIgnoreCase(pingResult) ? "HEALTHY" : "DEGRADED");
        stats.put("redisPing", pingResult);
        stats.put("serverInfo", serverProps);
        stats.put("memoryStats", memoryProps);

        // 2. Active Caches
        Collection<String> cacheNames = cacheManager.getCacheNames();
        stats.put("configuredCaches", cacheNames);

        // 3. Count Keys by namespace
        try {
            Set<String> allKeys = stringRedisTemplate.keys("crowdflow:*");
            stats.put("crowdflowKeyCount", allKeys != null ? allKeys.size() : 0);
        } catch (Exception e) {
            stats.put("crowdflowKeyCount", 0);
        }

        // 4. Geospatial Index Count
        Long indexedReports = redisGeoSpatialService.getIndexedReportCount();
        stats.put("geospatialIndexedReports", indexedReports);
        stats.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/clear")
    @Operation(summary = "Clear specific cache partition (wards, clusters_open, heatmap, live_tracking) or all")
    public ResponseEntity<Map<String, Object>> clearCache(@RequestParam(value = "cacheName", defaultValue = "all") String cacheName) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> cleared = new ArrayList<>();

        if ("all".equalsIgnoreCase(cacheName)) {
            for (String name : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                    cleared.add(name);
                }
            }
            result.put("message", "All Redis cache partitions successfully evicted");
        } else {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                cleared.add(cacheName);
                result.put("message", "Cache partition '" + cacheName + "' successfully evicted");
            } else {
                result.put("message", "Cache partition '" + cacheName + "' not found");
            }
        }

        result.put("evictedCaches", cleared);
        result.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/geo/nearby")
    @Operation(summary = "Query Redis Native Geospatial index for water issues within radius (sub-millisecond latency)")
    public ResponseEntity<Map<String, Object>> getNearbyReports(
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon,
            @RequestParam(value = "radiusKm", defaultValue = "1.0") Double radiusKm
    ) {
        long startTime = System.nanoTime();
        List<String> reportCodes = redisGeoSpatialService.findNearbyReportCodes(lat, lon, radiusKm);
        long durationMicros = (System.nanoTime() - startTime) / 1000;

        List<Map<String, Object>> nearbyDetails = new ArrayList<>();
        for (String code : reportCodes) {
            Optional<WaterReport> opt = reportRepository.findByReportCode(code);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reportCode", code);
            opt.ifPresent(report -> {
                item.put("issueType", report.getIssueType() != null ? report.getIssueType().name() : "OTHER");
                item.put("issueLabel", report.getIssueType() != null ? report.getIssueType().getDisplayName() : "Other");
                item.put("address", report.getAddress());
                item.put("wardNumber", report.getWardNumber());
                item.put("wardName", report.getWardName());
                item.put("latitude", report.getLatitude());
                item.put("longitude", report.getLongitude());
            });
            nearbyDetails.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("centerLatitude", lat);
        response.put("centerLongitude", lon);
        response.put("radiusKm", radiusKm);
        response.put("totalFound", reportCodes.size());
        response.put("queryDurationMicroseconds", durationMicros);
        response.put("reports", nearbyDetails);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Generate and store 6-digit OTP in Redis with 5-minute TTL for Indian mobile (+91)")
    public ResponseEntity<Map<String, Object>> requestOtp(@RequestParam("phone") String phone) {
        String otp = redisOtpService.generateAndSaveOtp(phone);
        long ttlSeconds = redisOtpService.getOtpTtlSeconds(phone);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("phone", phone);
        response.put("otpGenerated", otp); // Returned in dev/testing mode
        response.put("ttlSeconds", ttlSeconds);
        response.put("expiresIn", "5 Minutes");
        response.put("message", "OTP generated and stored in Redis with 5-minute TTL");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify submitted 6-digit OTP against Redis and invalidate upon success")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestParam("phone") String phone,
            @RequestParam("otp") String otp
    ) {
        boolean valid = redisOtpService.verifyOtp(phone, otp);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("phone", phone);
        response.put("verified", valid);
        response.put("message", valid
                ? "Mobile number successfully verified via Redis OTP"
                : "Invalid or expired OTP");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ratelimit/status")
    @Operation(summary = "Check atomic rate limiter status and reset TTL for a client IP or phone")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @RequestParam("clientKey") String clientKey,
            @RequestParam(value = "maxRequests", defaultValue = "30") int maxRequests
    ) {
        long remaining = rateLimiterService.getRemainingRequests(clientKey, maxRequests);
        long resetSeconds = rateLimiterService.getTimeToResetSeconds(clientKey);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("clientKey", clientKey);
        response.put("maxRequests", maxRequests);
        response.put("remainingRequests", remaining);
        response.put("resetInSeconds", resetSeconds);
        response.put("isAllowed", remaining > 0);

        return ResponseEntity.ok(response);
    }
}
