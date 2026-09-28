package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when supplied GPS coordinates fail validation (e.g. out of range, outside India/NCR envelope).
 */
public class InvalidCoordinateException extends WaterWatchException {

    public InvalidCoordinateException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_COORDINATES");
    }

    public InvalidCoordinateException(Double latitude, Double longitude, String reason) {
        super(
                String.format("Invalid GPS coordinates [%s, %s]: %s", latitude, longitude, reason),
                HttpStatus.BAD_REQUEST,
                "INVALID_COORDINATES",
                Map.of(
                        "latitude", latitude != null ? latitude : "null",
                        "longitude", longitude != null ? longitude : "null",
                        "reason", reason != null ? reason : "Unknown validation failure"
                )
        );
    }
}
