package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Generic 404 Resource Not Found exception.
 */
public class ResourceNotFoundException extends WaterWatchException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
                String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                Map.of("resource", resourceName, "field", fieldName, "value", String.valueOf(fieldValue))
        );
    }

    public ResourceNotFoundException(String message, String errorCode, Map<String, Object> details) {
        super(message, HttpStatus.NOT_FOUND, errorCode, details);
    }
}
