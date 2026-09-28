package com.civic.waterwatch.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Global Exception Handler & RFC 7807 Diagnostics Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test/resource");
    }

    @Test
    @DisplayName("Should translate ReportNotFoundException into 404 with REPORT_NOT_FOUND code")
    void testReportNotFoundException() {
        ReportNotFoundException ex = new ReportNotFoundException("IND-H2O-9999");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("REPORT_NOT_FOUND", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("IND-H2O-9999"));
        assertEquals("/api/test/resource", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertEquals("IND-H2O-9999", response.getBody().getDetails().get("reportCode"));
    }

    @Test
    @DisplayName("Should translate ClusterNotFoundException into 404 with CLUSTER_NOT_FOUND code")
    void testClusterNotFoundException() {
        ClusterNotFoundException ex = new ClusterNotFoundException(42L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CLUSTER_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals(42L, response.getBody().getDetails().get("clusterId"));
    }

    @Test
    @DisplayName("Should translate WardNotFoundException into 404 with WARD_NOT_FOUND code")
    void testWardNotFoundException() {
        WardNotFoundException ex = new WardNotFoundException(85);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("WARD_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals(85, response.getBody().getDetails().get("wardNumber"));
    }

    @Test
    @DisplayName("Should translate RateLimitExceededException into 429 with Retry-After header")
    void testRateLimitExceededException() {
        RateLimitExceededException ex = new RateLimitExceededException("+919810012345", 30, 60);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().getErrorCode());
        assertEquals("60", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals(60, response.getBody().getDetails().get("retryAfterSeconds"));
    }

    @Test
    @DisplayName("Should translate InvalidOtpException into 401 with INVALID_OTP code")
    void testInvalidOtpException() {
        InvalidOtpException ex = new InvalidOtpException("+919876543210");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidOtpException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_OTP", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("+919876543210"));
    }

    @Test
    @DisplayName("Should translate InvalidCoordinateException into 400 with details")
    void testInvalidCoordinateException() {
        InvalidCoordinateException ex = new InvalidCoordinateException(95.0, 190.0, "Latitude/Longitude outside Earth bounds");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_COORDINATES", response.getBody().getErrorCode());
        assertEquals(95.0, response.getBody().getDetails().get("latitude"));
        assertEquals(190.0, response.getBody().getDetails().get("longitude"));
    }

    @Test
    @DisplayName("Should translate FileValidationException into 400 with INVALID_FILE code")
    void testFileValidationException() {
        FileValidationException ex = new FileValidationException("malicious.php", "Executable file extensions are forbidden");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_FILE", response.getBody().getErrorCode());
        assertEquals("malicious.php", response.getBody().getDetails().get("filename"));
    }

    @Test
    @DisplayName("Should translate MediaStorageException into 500 with MEDIA_STORAGE_ERROR code")
    void testMediaStorageException() {
        MediaStorageException ex = new MediaStorageException("evidence.jpg", "Disk full on /uploads", new RuntimeException("No space left"));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MEDIA_STORAGE_ERROR", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate PdfGenerationException into 500 with PDF_GENERATION_FAILED code")
    void testPdfGenerationException() {
        PdfGenerationException ex = new PdfGenerationException(101L, new RuntimeException("PDF renderer error"));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PDF_GENERATION_FAILED", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate InvalidStatusTransitionException into 400 with allowed states")
    void testInvalidStatusTransitionException() {
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException(
                "RESOLVED", "INVESTIGATING", "[OPEN, ESCALATED, RESOLVED, CLOSED]"
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWaterWatchException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_STATUS_TRANSITION", response.getBody().getErrorCode());
        assertEquals("RESOLVED", response.getBody().getDetails().get("currentStatus"));
    }

    @Test
    @DisplayName("Should translate MethodArgumentNotValidException into 400 with field validation list")
    void testMethodArgumentNotValidException() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "reportRequest");
        bindingResult.addError(new FieldError("reportRequest", "issueType", null, false, null, null, "Issue type is required"));
        bindingResult.addError(new FieldError("reportRequest", "citizenPhone", "123", false, null, null, "Invalid Indian phone number"));

        java.lang.reflect.Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValid(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_FAILED", response.getBody().getErrorCode());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals(2, response.getBody().getValidationErrors().size());

        ErrorResponse.ValidationError field1 = response.getBody().getValidationErrors().get(0);
        assertEquals("issueType", field1.getField());
        assertEquals("Issue type is required", field1.getMessage());

        ErrorResponse.ValidationError field2 = response.getBody().getValidationErrors().get(1);
        assertEquals("citizenPhone", field2.getField());
        assertEquals("123", field2.getRejectedValue());
    }

    @Test
    @DisplayName("Should translate MethodArgumentTypeMismatchException into 400 with TYPE_MISMATCH code")
    void testTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "clusterId", null, null
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TYPE_MISMATCH", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("clusterId"));
        assertTrue(response.getBody().getMessage().contains("Long"));
    }

    @Test
    @DisplayName("Should translate MissingServletRequestParameterException into 400 with MISSING_PARAMETER code")
    void testMissingParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("issueType", "String");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MISSING_PARAMETER", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("issueType"));
    }

    @Test
    @DisplayName("Should translate MissingServletRequestPartException into 400 with MISSING_REQUEST_PART code")
    void testMissingRequestPartException() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("file");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingPart(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MISSING_REQUEST_PART", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("file"));
    }

    @Test
    @DisplayName("Should translate HttpMessageNotReadableException into 400 with MALFORMED_JSON_REQUEST code")
    void testMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error: Unexpected character");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MALFORMED_JSON_REQUEST", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate HttpRequestMethodNotSupportedException into 405 with METHOD_NOT_ALLOWED code")
    void testMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException(
                "GET", List.of("POST", "PATCH")
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("GET"));
    }

    @Test
    @DisplayName("Should translate HttpMediaTypeNotSupportedException into 415 with UNSUPPORTED_MEDIA_TYPE code")
    void testMediaTypeNotSupportedException() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMediaTypeNotSupported(ex, request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate MaxUploadSizeExceededException into 413 with FILE_SIZE_LIMIT_EXCEEDED code")
    void testMaxUploadSizeException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(25 * 1024 * 1024L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSize(ex, request);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FILE_SIZE_LIMIT_EXCEEDED", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate DataIntegrityViolationException into 409 with DATABASE_CONFLICT code")
    void testDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique index violation: duplicate reportCode");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATABASE_CONFLICT", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate AccessDeniedException into 403 with ACCESS_DENIED code")
    void testAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("User has insufficient role");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate AuthenticationException into 401 with AUTHENTICATION_REQUIRED code")
    void testAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHENTICATION_REQUIRED", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should translate IllegalArgumentException into 400 with ILLEGAL_ARGUMENT code")
    void testIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Rating must be between 1 and 5");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ILLEGAL_ARGUMENT", response.getBody().getErrorCode());
        assertEquals("Rating must be between 1 and 5", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should translate NoResourceFoundException into 404 with ENDPOINT_NOT_FOUND code")
    void testNoResourceFoundException() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/path");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENDPOINT_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Should handle unexpected generic exception with 500 and unique correlation traceId")
    void testGenericException() {
        NullPointerException ex = new NullPointerException("Simulated unexpected null reference");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertNotNull(response.getBody().getTraceId());
        assertTrue(response.getBody().getMessage().contains("trace ID"));
        assertFalse(response.getBody().getMessage().contains("NullPointerException")); // No internal leak
    }
}
