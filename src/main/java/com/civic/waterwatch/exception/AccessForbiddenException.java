package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an authenticated user does not have sufficient role or permission
 * to perform a specific municipal operation (e.g. citizen attempting ward officer dispatch).
 */
public class AccessForbiddenException extends WaterWatchException {

    public AccessForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION");
    }

    public AccessForbiddenException(String requiredRole, String userRole) {
        super(
                String.format("Operation requires authority '%s', but current user holds '%s'", requiredRole, userRole),
                HttpStatus.FORBIDDEN,
                "FORBIDDEN_OPERATION",
                Map.of("requiredRole", requiredRole, "userRole", userRole != null ? userRole : "NONE")
        );
    }
}
