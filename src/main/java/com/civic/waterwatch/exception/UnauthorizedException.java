package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication is missing or invalid.
 */
public class UnauthorizedException extends WaterWatchException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_ACCESS");
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }
}
