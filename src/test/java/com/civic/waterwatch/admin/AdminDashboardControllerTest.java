package com.civic.waterwatch.admin;

import com.civic.waterwatch.WaterWatchApplication;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = WaterWatchApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Admin Dashboard RBAC & Control Room Integration Tests")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WaterReportRepository reportRepository;

    private String officerToken;
    private String adminToken;
    private String citizenToken;

    @BeforeEach
    void setUp() {
        officerToken = "Bearer " + jwtTokenProvider.generateToken("+919811023412", List.of("ROLE_WARD_OFFICER"));
        adminToken = "Bearer " + jwtTokenProvider.generateToken("+919871154321", List.of("ROLE_SUPER_ADMIN"));
        citizenToken = "Bearer " + jwtTokenProvider.generateToken("+919123456789", List.of("ROLE_CITIZEN"));
    }

    @Test
    @DisplayName("Unauthenticated request to /api/admin/overview should return 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("Citizen role (ROLE_CITIZEN) accessing /api/admin/overview should return 403 Forbidden")
    void testCitizenRoleForbiddenFromAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", citizenToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Ward Officer (ROLE_WARD_OFFICER) should successfully fetch /api/admin/overview KPIs")
    void testWardOfficerCanAccessOverview() throws Exception {
        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", officerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalReports").isNumber())
                .andExpect(jsonPath("$.activeTankersCount").isNumber())
                .andExpect(jsonPath("$.totalDepotsCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.systemStatus").value("OPERATIONAL"));
    }

    @Test
    @DisplayName("Super Admin (ROLE_SUPER_ADMIN) should successfully fetch /api/admin/overview KPIs")
    void testSuperAdminCanAccessOverview() throws Exception {
        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalReports").isNumber())
                .andExpect(jsonPath("$.systemStatus").value("OPERATIONAL"));
    }

    @Test
    @DisplayName("Officer should filter /api/admin/reports by status and search keyword")
    void testFilterReports() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", officerToken)
                        .param("status", "SUBMITTED")
                        .param("search", "Karol Bagh"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Officer can update report status to IN_PROGRESS with field notes")
    void testUpdateReportStatus() throws Exception {
        // Create a test report
        WaterReport testReport = new WaterReport();
        testReport.setReportCode("IND-TEST-" + System.currentTimeMillis());
        testReport.setIssueType(IssueType.BURST_PIPE);
        testReport.setLatitude(28.6445);
        testReport.setLongitude(77.1950);
        testReport.setStatus(ReportStatus.SUBMITTED);
        testReport.setReportedAt(LocalDateTime.now());
        testReport = reportRepository.save(testReport);

        mockMvc.perform(patch("/api/admin/reports/" + testReport.getId() + "/status")
                        .header("Authorization", officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\", \"notes\": \"Dispatched DJB repair crew 4B to site\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.statusNotes").value("Dispatched DJB repair crew 4B to site"));

        // Clean up
        reportRepository.delete(testReport);
    }

    @Test
    @DisplayName("Officer should view fleet status and depots")
    void testGetFleetStatus() throws Exception {
        mockMvc.perform(get("/api/admin/fleet/status")
                        .header("Authorization", officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depots").isArray())
                .andExpect(jsonPath("$.depots", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.tankers").isArray());
    }

    @Test
    @DisplayName("Admin can view system diagnostics")
    void testGetSystemDiagnostics() throws Exception {
        mockMvc.perform(get("/api/admin/diagnostics")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databaseConnected").value(true))
                .andExpect(jsonPath("$.jvmUsedMemoryMb").isNumber())
                .andExpect(jsonPath("$.uptimeSeconds").isNumber());
    }
}
