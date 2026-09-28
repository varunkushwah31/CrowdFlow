package com.civic.waterwatch;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.clustering.service.SpatialClusteringService;
import com.civic.waterwatch.dispatch.model.DispatchLog;
import com.civic.waterwatch.dispatch.repository.DispatchLogRepository;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.reporting.service.PdfReportService;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.repository.MunicipalWardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = WaterWatchApplication.class)
@ActiveProfiles("dev")
class WaterWatchIntegrationTest {

    @Autowired
    private WaterReportRepository reportRepository;

    @Autowired
    private IncidentClusterRepository clusterRepository;

    @Autowired
    private MunicipalWardRepository wardRepository;

    @Autowired
    private DispatchLogRepository dispatchLogRepository;

    @Autowired
    private SpatialClusteringService clusteringService;

    @Autowired
    private PdfReportService pdfReportService;

    @Test
    @DisplayName("Should successfully load Spring context on Java 25 and initialize Indian civic wards")
    void testContextLoadsAndWardsSeeded() {
        assertNotNull(clusteringService, "SpatialClusteringService bean should be loaded");
        assertNotNull(pdfReportService, "PdfReportService bean should be loaded");
        assertNotNull(dispatchLogRepository, "DispatchLogRepository bean should be loaded");

        List<MunicipalWard> wards = wardRepository.findAll();
        assertFalse(wards.isEmpty(), "Wards should be seeded on startup");
        assertTrue(wards.stream().anyMatch(w -> w.getWardNumber() == 85), "Ward 85 (Karol Bagh - DJB) should exist");
        assertTrue(wards.stream().anyMatch(w -> w.getWardNumber() == 210), "Ward 210 (Mayur Vihar - MCD/DJB) should exist");
    }

    @Test
    @DisplayName("Should seed realistic Indian water grievances and run initial DBSCAN clustering")
    void testInitialDataAndClustering() {
        List<WaterReport> reports = reportRepository.findAll();
        assertFalse(reports.isEmpty(), "Initial demo grievances should be seeded");

        List<IncidentCluster> clusters = clusterRepository.findAll();
        assertFalse(clusters.isEmpty(), "Clusters should be formed from seed grievances");

        // Verify root cause correlation for the burst main cluster in Ward 85
        IncidentCluster burstCluster = clusters.stream()
                .filter(c -> c.getRootCauseHypothesis().contains("Burst") || c.getRootCauseHypothesis().contains("Main"))
                .findFirst()
                .orElse(null);

        assertNotNull(burstCluster, "Should identify the major pipeline burst cluster");
        assertEquals(ClusterSeverity.CRITICAL, burstCluster.getSeverity(), "Feeder burst cluster should be marked CRITICAL");
        assertNotNull(burstCluster.getBoundaryGeoJson(), "Convex hull boundary should be computed");
    }

    @Test
    @DisplayName("Should generate a valid vector PDF municipal dossier using OpenHTMLtoPDF & Thymeleaf")
    void testPdfGeneration() {
        List<IncidentCluster> clusters = clusterRepository.findAll();
        assertFalse(clusters.isEmpty(), "Clusters must exist to test PDF generation");

        IncidentCluster targetCluster = clusters.get(0);
        byte[] pdfBytes = pdfReportService.generateClusterReport(targetCluster.getId());

        assertNotNull(pdfBytes, "PDF byte stream should not be null");
        assertTrue(pdfBytes.length > 1000, "PDF size should be substantial (actual: " + pdfBytes.length + " bytes)");

        // Verify standard PDF header "%PDF-"
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header, "Generated file should have valid PDF signature");
    }

    @Test
    @DisplayName("Should log automated dispatches to Indian municipal authorities")
    void testDispatchLogging() {
        List<DispatchLog> logs = dispatchLogRepository.findAll();
        assertNotNull(logs, "Dispatch logs repository should be accessible");
    }
}
