package com.civic.waterwatch.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("CrowdFlow India Redis Real Integration & Operations Tests")
class RedisFunctionalityTest {

    @Autowired
    private RedisOtpService otpService;

    @Autowired
    private RedisRateLimiterService rateLimiterService;

    @Autowired
    private RedisGeoSpatialService geoService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanUpTestKeys() {
        try {
            Set<String> testKeys = stringRedisTemplate.keys("crowdflow:otp:+9198110*");
            if (testKeys != null && !testKeys.isEmpty()) {
                stringRedisTemplate.delete(testKeys);
            }
            Set<String> rateKeys = stringRedisTemplate.keys("crowdflow:ratelimit:test-*");
            if (rateKeys != null && !rateKeys.isEmpty()) {
                stringRedisTemplate.delete(rateKeys);
            }
            geoService.removeReportLocation("TEST-GEO-001");
            geoService.removeReportLocation("TEST-GEO-002");
            geoService.removeReportLocation("TEST-GEO-003");
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("RedisOtpService: Should generate 6-digit OTP, store in Redis with TTL, verify, and prevent replay")
    void testRedisOtpService() {
        String phone = "+919811099887";
        String otp = otpService.generateAndSaveOtp(phone);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("^[0-9]{6}$"), "OTP must be 6 numeric digits");

        // Verify TTL in Redis
        long ttl = otpService.getOtpTtlSeconds(phone);
        assertTrue(ttl > 0 && ttl <= 300, "TTL should be set close to 300 seconds");

        // Wrong OTP must fail
        assertFalse(otpService.verifyOtp(phone, "000000"), "Wrong OTP must fail");

        // Correct OTP must succeed
        assertTrue(otpService.verifyOtp(phone, otp), "Correct OTP must succeed");

        // Replay attempt must fail as key was deleted
        assertFalse(otpService.verifyOtp(phone, otp), "Replay attempt must fail");
    }

    @Test
    @DisplayName("RedisRateLimiterService: Should enforce rate limit thresholds atomically in Redis")
    void testRedisRateLimiterService() {
        String clientKey = "test-client-delhi-" + System.currentTimeMillis();
        int maxRequests = 4;
        int windowSeconds = 10;

        // First 4 requests must pass
        for (int i = 1; i <= maxRequests; i++) {
            assertTrue(rateLimiterService.isAllowed(clientKey, maxRequests, windowSeconds),
                    "Request " + i + " within limit should be allowed");
        }

        // 5th request must be rejected
        assertFalse(rateLimiterService.isAllowed(clientKey, maxRequests, windowSeconds),
                "Request exceeding limit must be blocked by Redis rate limiter");

        assertEquals(0, rateLimiterService.getRemainingRequests(clientKey, maxRequests));
        assertTrue(rateLimiterService.getTimeToResetSeconds(clientKey) > 0);
    }

    @Test
    @DisplayName("RedisGeoSpatialService: Should index coordinates in Redis Geo and execute radius queries")
    void testRedisGeoSpatialService() {
        // Index Karol Bagh reports (Ward 85 - Central Delhi)
        geoService.indexReportLocation("TEST-GEO-001", 28.6445, 77.1950);
        geoService.indexReportLocation("TEST-GEO-002", 28.6450, 77.1955);

        // Index Mayur Vihar report (Ward 210 - East Delhi, ~12 km away)
        geoService.indexReportLocation("TEST-GEO-003", 28.6080, 77.2980);

        assertTrue(geoService.getIndexedReportCount() >= 3);

        // Proximity query: 1.0 km radius around Karol Bagh
        List<String> nearby = geoService.findNearbyReportCodes(28.6445, 77.1950, 1.0);
        assertNotNull(nearby);
        assertTrue(nearby.contains("TEST-GEO-001"), "Should find TEST-GEO-001");
        assertTrue(nearby.contains("TEST-GEO-002"), "Should find TEST-GEO-002");
        assertFalse(nearby.contains("TEST-GEO-003"), "Should not find distant East Delhi report");

        // Remove test locations
        geoService.removeReportLocation("TEST-GEO-001");
        geoService.removeReportLocation("TEST-GEO-002");
        geoService.removeReportLocation("TEST-GEO-003");
    }

    @Test
    @DisplayName("RedisPubSubService: Should serialize and deserialize IncidentEventMessage accurately")
    void testPubSubEventMessageSerialization() throws Exception {
        RedisPubSubService.IncidentEventMessage event = new RedisPubSubService.IncidentEventMessage(
                "IND-H2O-1686", 42L, "STATUS_UPDATED", "2026-09-26T17:00:00"
        );

        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("IND-H2O-1686"));
        assertTrue(json.contains("STATUS_UPDATED"));

        RedisPubSubService.IncidentEventMessage deserialized =
                objectMapper.readValue(json, RedisPubSubService.IncidentEventMessage.class);

        assertEquals(event.getReportCode(), deserialized.getReportCode());
        assertEquals(event.getClusterId(), deserialized.getClusterId());
        assertEquals(event.getEventType(), deserialized.getEventType());
        assertEquals(event.getTimestamp(), deserialized.getTimestamp());
    }
}
