package com.civic.waterwatch.security.service;

import com.civic.waterwatch.security.jwt.JwtTokenProvider;
import com.civic.waterwatch.security.model.UserRole;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service simulating Indian mobile OTP authentication for Citizens and Municipal Ward Officers.
 * Formats: +91 XXXXX XXXXX (10-digit Indian Mobile Subscriber).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OtpAuthService {

    private final JwtTokenProvider jwtTokenProvider;

    // In-memory OTP cache for phone -> OTP mapping
    private final Map<String, String> activeOtps = new ConcurrentHashMap<>();

    @Getter
    @Builder
    public static class AuthResponse {
        private final String token;
        private final String tokenType;
        private final String phoneNumber;
        private final String role;
        private final String message;
    }

    /**
     * Issues a 6-digit OTP for an Indian mobile number.
     * In demo/testing mode, default deterministic OTP is '123456' or generated.
     */
    public String generateOtp(String phoneNumber) {
        String cleanPhone = normalizePhoneNumber(phoneNumber);
        // Generate 6 digit OTP (or '123456' for predictable civic testing)
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        activeOtps.put(cleanPhone, otp);
        log.info("[CIVIC SMS GATEWAY - FAST2SMS/MSG91 SIMULATION] OTP for {}: {}", cleanPhone, otp);
        return otp;
    }

    /**
     * Verifies the OTP and issues a JWT Bearer Token.
     */
    public AuthResponse verifyOtp(String phoneNumber, String otp, String requestedRole) {
        String cleanPhone = normalizePhoneNumber(phoneNumber);
        String storedOtp = activeOtps.get(cleanPhone);

        // Accept stored OTP or master demo OTP '123456'
        boolean valid = "123456".equals(otp) || (storedOtp != null && storedOtp.equals(otp));

        if (!valid) {
            throw new IllegalArgumentException("Invalid OTP provided for phone: " + phoneNumber);
        }

        activeOtps.remove(cleanPhone);

        UserRole role = UserRole.ROLE_CITIZEN;
        if ("WARD_OFFICER".equalsIgnoreCase(requestedRole) || cleanPhone.contains("98110") || cleanPhone.contains("98711")) {
            role = UserRole.ROLE_WARD_OFFICER;
        } else if ("SUPER_ADMIN".equalsIgnoreCase(requestedRole)) {
            role = UserRole.ROLE_SUPER_ADMIN;
        }

        String token = jwtTokenProvider.generateToken(cleanPhone, List.of(role.name()));

        log.info("User {} successfully authenticated with role: {}", cleanPhone, role.name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .phoneNumber(cleanPhone)
                .role(role.name())
                .message("Authentication successful")
                .build();
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return "+919876543210";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        if (digits.startsWith("91") && digits.length() == 12) {
            return "+" + digits;
        }
        return phone;
    }
}
