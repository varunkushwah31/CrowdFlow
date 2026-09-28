package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an illegal lifecycle status transition is attempted on an incident report or cluster.
 */
public class InvalidStatusTransitionException extends WaterWatchException {

    public InvalidStatusTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION");
    }

    public InvalidStatusTransitionException(String currentStatus, String targetStatus, String allowedValues) {
        super(
                String.format("Cannot transition status from '%s' to '%s'. Allowed values: %s", currentStatus, targetStatus, allowedValues),
                HttpStatus.BAD_REQUEST,
                "INVALID_STATUS_TRANSITION",
                Map.of("currentStatus", currentStatus, "targetStatus", targetStatus, "allowedValues", allowedValues)
        );
    }
}
