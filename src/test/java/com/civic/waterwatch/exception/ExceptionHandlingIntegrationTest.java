package com.civic.waterwatch.exception;

import com.civic.waterwatch.WaterWatchApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = WaterWatchApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Spring MVC Exception Handling & Error Pipeline Integration Tests")
class ExceptionHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 404 JSON for nonexistent report tracking code")
    void testTrackReportNotFound() throws Exception {
        mockMvc.perform(get("/api/reports/track/IND-H2O-NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("IND-H2O-NONEXISTENT")))
                .andExpect(jsonPath("$.path").value("/api/reports/track/IND-H2O-NONEXISTENT"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 404 JSON for nonexistent incident cluster ID")
    void testClusterNotFound() throws Exception {
        mockMvc.perform(get("/api/clusters/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("CLUSTER_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 404 JSON for nonexistent municipal ward number")
    void testWardNotFound() throws Exception {
        mockMvc.perform(get("/api/wards/9999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("WARD_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid coordinate dispatch parameters")
    void testCoordinateDispatchInvalidBounds() throws Exception {
        mockMvc.perform(get("/api/geo/coordinate-dispatch")
                        .param("lat", "125.0") // Out of [-90, 90] bounds
                        .param("lon", "77.2100"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_COORDINATES"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when missing required query parameters in distance calculation")
    void testMissingQueryParameter() throws Exception {
        mockMvc.perform(get("/api/geo/distance")
                        .param("lat1", "28.6139")) // Missing lon1, lat2, lon2
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for malformed JSON request body")
    void testMalformedJsonPayload() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json body: 123 }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON_REQUEST"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when Bean Validation fails on report submission")
    void testBeanValidationFailure() throws Exception {
        // Missing required issueType field
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Water pipe burst\", \"latitude\": 28.61}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors", notNullValue()))
                .andExpect(jsonPath("$.validationErrors[0].field").value("issueType"));
    }

    @Test
    @DisplayName("Should return 405 Method Not Allowed when executing unsupported HTTP verb")
    void testMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/reports"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when type mismatch occurs on path variable")
    void testTypeMismatchOnPathVariable() throws Exception {
        mockMvc.perform(get("/api/clusters/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for landmark search with insufficient characters")
    void testLandmarkSearchValidation() throws Exception {
        mockMvc.perform(get("/api/geo/search").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("ILLEGAL_ARGUMENT"))
                .andExpect(jsonPath("$.message").value(containsString("at least 2 characters")));
    }
}
