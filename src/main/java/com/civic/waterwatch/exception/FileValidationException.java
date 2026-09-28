package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an uploaded file is invalid (e.g. empty, forbidden path traversal, unsupported format).
 */
public class FileValidationException extends WaterWatchException {

    public FileValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
    }

    public FileValidationException(String filename, String reason) {
        super(
                String.format("File validation failed for '%s': %s", filename, reason),
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE",
                Map.of("filename", filename != null ? filename : "unknown", "reason", reason)
        );
    }
}
