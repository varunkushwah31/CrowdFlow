package com.civic.waterwatch.security.controller;

import com.civic.waterwatch.security.service.OtpAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Access Control (India Civic)", description = "OTP-based mobile login for Indian citizens and municipal ward officers")
public class AuthController {

    private final OtpAuthService otpAuthService;

    @Data
    public static class OtpRequest {
        private String phoneNumber;
    }

    @Data
    public static class OtpVerifyRequest {
        private String phoneNumber;
        private String otp;
        private String role; // "CITIZEN", "WARD_OFFICER", "SUPER_ADMIN"
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send 6-digit OTP to citizen/officer Indian phone (+91)")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody OtpRequest request) {
        String otp = otpAuthService.generateOtp(request.getPhoneNumber());
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "OTP dispatched to " + request.getPhoneNumber(),
                "demoOtp", otp // Included for quick interactive evaluation
        ));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and obtain JWT Bearer token with RBAC permissions")
    public ResponseEntity<OtpAuthService.AuthResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
        OtpAuthService.AuthResponse response = otpAuthService.verifyOtp(
                request.getPhoneNumber(),
                request.getOtp(),
                request.getRole()
        );
        return ResponseEntity.ok(response);
    }
}
