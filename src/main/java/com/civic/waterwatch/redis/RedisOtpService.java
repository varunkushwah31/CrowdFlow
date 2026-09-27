package com.civic.waterwatch.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Citizen OTP Verification Service backed by Redis.
 * Enforces 5-minute expiration (TTL) and secure verification for Indian mobile numbers (+91).
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisOtpService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom random = new SecureRandom();

    private static final String OTP_PREFIX = "crowdflow:otp:";
    private static final long OTP_TTL_SECONDS = 300; // 5 minutes

    /**
     * Generates a 6-digit numeric OTP and stores it in Redis with a 5-minute TTL.
     *
     * @param phoneNumber Citizen Indian mobile number
     * @return 6-digit OTP string
     */
    public String generateAndSaveOtp(String phoneNumber) {
        String normalizedPhone = normalizePhone(phoneNumber);
        int code = 100000 + random.nextInt(900000);
        String otp = String.valueOf(code);

        String redisKey = OTP_PREFIX + normalizedPhone;
        try {
            stringRedisTemplate.opsForValue().set(redisKey, otp, Duration.ofSeconds(OTP_TTL_SECONDS));
            log.info("Generated 6-digit OTP for citizen phone {}: [TTL: {}s]", normalizedPhone, OTP_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to store OTP in Redis for phone {}: {}", normalizedPhone, e.getMessage());
        }
        return otp;
    }

    /**
     * Validates an OTP submitted by a citizen. If valid, deletes the OTP from Redis to prevent replay.
     */
    public boolean verifyOtp(String phoneNumber, String inputOtp) {
        if (inputOtp == null || inputOtp.isBlank()) {
            return false;
        }

        String normalizedPhone = normalizePhone(phoneNumber);
        String redisKey = OTP_PREFIX + normalizedPhone;

        try {
            String storedOtp = stringRedisTemplate.opsForValue().get(redisKey);
            if (storedOtp != null && storedOtp.equals(inputOtp.trim())) {
                // Delete upon successful verification to avoid replay
                stringRedisTemplate.delete(redisKey);
                log.info("Citizen OTP verification SUCCEEDED for phone {}", normalizedPhone);
                return true;
            }
        } catch (Exception e) {
            log.warn("Failed to verify OTP against Redis for phone {}: {}", normalizedPhone, e.getMessage());
        }

        log.warn("Citizen OTP verification FAILED for phone {}", normalizedPhone);
        return false;
    }

    /**
     * Manually invalidates any pending OTP for the given phone number.
     */
    public void revokeOtp(String phoneNumber) {
        try {
            stringRedisTemplate.delete(OTP_PREFIX + normalizePhone(phoneNumber));
        } catch (Exception e) {
            log.warn("Failed to revoke OTP in Redis: {}", e.getMessage());
        }
    }

    /**
     * Returns remaining TTL of an OTP in seconds.
     */
    public long getOtpTtlSeconds(String phoneNumber) {
        try {
            Long ttl = stringRedisTemplate.getExpire(OTP_PREFIX + normalizePhone(phoneNumber));
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception _) {
            return 0;
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "unknown";
        return phone.replaceAll("[^0-9+]", "");
    }
}
