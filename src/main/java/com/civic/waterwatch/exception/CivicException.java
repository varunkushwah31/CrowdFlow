package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Interface contract for all domain and civic business exceptions
 * across the WaterWatch India platform.
 */
public interface CivicException {

    /**
     * Associated HTTP response status.
     */
    HttpStatus getStatus();

    /**
     * Machine-readable civic error code (e.g. "REPORT_NOT_FOUND", "INVALID_OTP").
     */
    String getErrorCode();

    /**
     * Human-readable diagnostic or citizen message.
     */
    String getMessage();

    /**
     * Additional structured metadata, field violations, or context details.
     */
    default Map<String, Object> getDetails() {
        return Collections.emptyMap();
    }
}
