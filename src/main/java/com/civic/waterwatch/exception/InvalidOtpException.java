package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Thrown when an invalid or expired mobile OTP is submitted during citizen/officer authentication.
 * Extends IllegalArgumentException for backward compatibility with existing tests and clients.
 */
public class InvalidOtpException extends IllegalArgumentException implements CivicException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public InvalidOtpException(String phoneNumber) {
        super("Invalid OTP provided for phone: " + phoneNumber);
        this.status = HttpStatus.UNAUTHORIZED;
        this.errorCode = "INVALID_OTP";
        this.details = Map.of("phoneNumber", phoneNumber != null ? phoneNumber : "unknown");
    }

    public InvalidOtpException(String phoneNumber, String customMessage) {
        super(customMessage);
        this.status = HttpStatus.UNAUTHORIZED;
        this.errorCode = "INVALID_OTP";
        this.details = Map.of("phoneNumber", phoneNumber != null ? phoneNumber : "unknown");
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details != null ? details : Collections.emptyMap();
    }
}
