package com.civic.waterwatch.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standardized API Error Response conforming to RFC 7807 Problem Details
 * and enterprise civic platform diagnostics.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private int status;
    private String error;
    private String errorCode;
    private String message;
    private String path;

    @Builder.Default
    private String traceId = UUID.randomUUID().toString();

    private Map<String, Object> details;
    private List<ValidationError> validationErrors;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private Object rejectedValue;
        private String message;
    }

    public static ErrorResponse of(HttpStatus status, String errorCode, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .traceId(UUID.randomUUID().toString())
                .build();
    }

    public static ErrorResponse of(CivicException ex, String path) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> details = ex.getDetails();

        return ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(path)
                .traceId(UUID.randomUUID().toString())
                .details(details != null && !details.isEmpty() ? details : null)
                .build();
    }

    public static ErrorResponse ofValidationErrors(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            List<ValidationError> validationErrors
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .traceId(UUID.randomUUID().toString())
                .validationErrors(validationErrors != null && !validationErrors.isEmpty() ? validationErrors : null)
                .build();
    }
}
