package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an error occurs during the generation of the official municipal PDF incident dossier.
 */
public class PdfGenerationException extends WaterWatchException {

    public PdfGenerationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED");
    }

    public PdfGenerationException(Long clusterId, Throwable cause) {
        super(
                "Failed to generate municipal PDF dossier for cluster ID: " + clusterId,
                cause,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PDF_GENERATION_FAILED",
                Map.of("clusterId", clusterId)
        );
    }
}
