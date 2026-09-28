package com.civic.waterwatch.exception;

import com.civic.waterwatch.security.jwt.CustomAccessDeniedHandler;
import com.civic.waterwatch.security.jwt.JwtAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security Exception Handling & Custom Error Controller Unit Tests")
class SecurityAndErrorControllerTest {

    private ObjectMapper objectMapper;
    private JwtAuthenticationEntryPoint authenticationEntryPoint;
    private CustomAccessDeniedHandler accessDeniedHandler;
    private CustomErrorController errorController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticationEntryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
        errorController = new CustomErrorController();
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint should write 401 Problem Details JSON")
    void testAuthenticationEntryPointCommence() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/clusters/purge");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Full authentication is required")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType() != null && response.getContentType().startsWith("application/json"));

        String json = response.getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        assertEquals(401, root.get("status").asInt());
        assertEquals("AUTHENTICATION_REQUIRED", root.get("errorCode").asText());
        assertEquals("/api/admin/clusters/purge", root.get("path").asText());
        assertTrue(root.has("traceId"));
        assertTrue(root.has("timestamp"));
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler should write 403 Problem Details JSON")
    void testAccessDeniedHandler() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/clusters/10/escalate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("User does not have role WARD_OFFICER")
        );

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType() != null && response.getContentType().startsWith("application/json"));

        String json = response.getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        assertEquals(403, root.get("status").asInt());
        assertEquals("ACCESS_DENIED", root.get("errorCode").asText());
        assertEquals("/api/clusters/10/escalate", root.get("path").asText());
        assertTrue(root.has("traceId"));
    }

    @Test
    @DisplayName("CustomErrorController should handle 404 container dispatch")
    void testCustomErrorController404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/non-existent-api");
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Not Found");

        ResponseEntity<ErrorResponse> response = errorController.handleError(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("ENDPOINT_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("/non-existent-api", response.getBody().getPath());
    }

    @Test
    @DisplayName("CustomErrorController should handle 500 internal server container error")
    void testCustomErrorController500() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/crash");
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Internal server error");

        ResponseEntity<ErrorResponse> response = errorController.handleError(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }
}
