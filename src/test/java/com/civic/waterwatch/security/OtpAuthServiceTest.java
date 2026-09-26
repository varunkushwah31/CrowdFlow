package com.civic.waterwatch.security;

import com.civic.waterwatch.security.jwt.JwtTokenProvider;
import com.civic.waterwatch.security.model.UserRole;
import com.civic.waterwatch.security.service.OtpAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Indian Mobile OTP Authentication & JWT RBAC Unit Tests")
class OtpAuthServiceTest {

    private OtpAuthService otpAuthService;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "4c7a6e9f1d2b8c5e3a7f9b0c2d4e6f8a1b3c5e7d9f0a2b4c6e8d0f2a4b6c8e0",
                3600000
        );
        otpAuthService = new OtpAuthService(jwtTokenProvider);
    }

    @Test
    @DisplayName("Should generate 6-digit OTP and successfully authenticate Indian Citizen")
    void testCitizenOtpFlow() {
        String phone = "+919810012345";
        String otp = otpAuthService.generateOtp(phone);
        assertNotNull(otp);
        assertEquals(6, otp.length());

        OtpAuthService.AuthResponse response = otpAuthService.verifyOtp(phone, otp, "CITIZEN");
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(UserRole.ROLE_CITIZEN.name(), response.getRole());

        // Validate generated JWT
        assertTrue(jwtTokenProvider.validateToken(response.getToken()));
        assertEquals(phone, jwtTokenProvider.getUsernameFromToken(response.getToken()));
        assertTrue(jwtTokenProvider.getRolesFromToken(response.getToken()).contains("ROLE_CITIZEN"));
    }

    @Test
    @DisplayName("Should assign WARD_OFFICER role when requested by municipal engineering personnel")
    void testWardOfficerOtpFlow() {
        String phone = "+919811023412"; // Karol Bagh EE Alok Sharma
        otpAuthService.generateOtp(phone);

        OtpAuthService.AuthResponse response = otpAuthService.verifyOtp(phone, "123456", "WARD_OFFICER");
        assertEquals(UserRole.ROLE_WARD_OFFICER.name(), response.getRole());
        assertTrue(jwtTokenProvider.getRolesFromToken(response.getToken()).contains("ROLE_WARD_OFFICER"));
    }

    @Test
    @DisplayName("Should reject invalid OTP with IllegalArgumentException")
    void testInvalidOtpRejection() {
        String phone = "+919876543210";
        otpAuthService.generateOtp(phone);

        assertThrows(IllegalArgumentException.class, () -> {
            otpAuthService.verifyOtp(phone, "999999", "CITIZEN");
        });
    }
}
