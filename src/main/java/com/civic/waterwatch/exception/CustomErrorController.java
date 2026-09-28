package com.civic.waterwatch.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback ErrorController that guarantees uniform JSON error payloads for any servlet container,
 * proxy, or filter-level dispatches directed to /error.
 */
@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String message = (messageObj != null && !messageObj.toString().isBlank())
                ? messageObj.toString()
                : status.getReasonPhrase();

        Object requestUriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = requestUriObj != null ? requestUriObj.toString() : request.getRequestURI();

        String errorCode = switch (status) {
            case NOT_FOUND -> "ENDPOINT_NOT_FOUND";
            case UNAUTHORIZED -> "AUTHENTICATION_REQUIRED";
            case FORBIDDEN -> "ACCESS_DENIED";
            case BAD_REQUEST -> "BAD_REQUEST";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case TOO_MANY_REQUESTS -> "RATE_LIMIT_EXCEEDED";
            default -> "INTERNAL_SERVER_ERROR";
        };

        ErrorResponse errorResponse = ErrorResponse.of(status, errorCode, message, path);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
