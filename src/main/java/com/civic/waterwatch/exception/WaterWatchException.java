package com.civic.waterwatch.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base abstract runtime exception for the WaterWatch platform.
 * Carries an HTTP status, a machine-readable civic error code, and optional structured metadata.
 */
@Getter
public class WaterWatchException extends RuntimeException implements CivicException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public WaterWatchException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", Collections.emptyMap());
    }

    public WaterWatchException(String message, HttpStatus status, String errorCode) {
        this(message, status, errorCode, Collections.emptyMap());
    }

    public WaterWatchException(String message, HttpStatus status, String errorCode, Map<String, Object> details) {
        super(message);
        this.status = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = errorCode != null ? errorCode : "INTERNAL_ERROR";
        this.details = details != null ? new LinkedHashMap<>(details) : Collections.emptyMap();
    }

    public WaterWatchException(String message, Throwable cause, HttpStatus status, String errorCode) {
        this(message, cause, status, errorCode, Collections.emptyMap());
    }

    public WaterWatchException(String message, Throwable cause, HttpStatus status, String errorCode, Map<String, Object> details) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = errorCode != null ? errorCode : "INTERNAL_ERROR";
        this.details = details != null ? new LinkedHashMap<>(details) : Collections.emptyMap();
    }
}
