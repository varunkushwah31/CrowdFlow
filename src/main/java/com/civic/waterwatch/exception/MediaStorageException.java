package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an error occurs during media storage, file read/write, or S3/MinIO operations.
 */
public class MediaStorageException extends WaterWatchException {

    public MediaStorageException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_STORAGE_ERROR");
    }

    public MediaStorageException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_STORAGE_ERROR");
    }

    public MediaStorageException(String filename, String reason, Throwable cause) {
        super(
                String.format("Storage failure for file '%s': %s", filename, reason),
                cause,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "MEDIA_STORAGE_ERROR",
                Map.of("filename", filename != null ? filename : "unknown", "reason", reason)
        );
    }
}
