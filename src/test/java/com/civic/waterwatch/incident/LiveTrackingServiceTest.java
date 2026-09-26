package com.civic.waterwatch.incident;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.incident.service.LiveTrackingService;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Citizen Live Grievance Tracking & Real-Time Milestones Test")
class LiveTrackingServiceTest {

    private WaterReport dummyReport;
    private IncidentCluster dummyCluster;
    private MunicipalWard dummyWard;
    private LiveTrackingService liveTrackingService;

    @BeforeEach
    void setUp() {
        dummyReport = new WaterReport();
        dummyReport.setId(101L);
        dummyReport.setReportCode("IND-H2O-9999");
        dummyReport.setIssueType(IssueType.BURST_PIPE);
        dummyReport.setDescription("450mm feeder main burst at Pusa Road");
        dummyReport.setLatitude(28.6445);
        dummyReport.setLongitude(77.1950);
        dummyReport.setAddress("Pusa Road, Karol Bagh");
        dummyReport.setWardNumber(85);
        dummyReport.setMunicipalBody("Delhi Jal Board (DJB)");
        dummyReport.setStatus(ReportStatus.CLUSTERED);
        dummyReport.setClusterId(55L);
        dummyReport.setReportedAt(LocalDateTime.now().minusHours(2));

        dummyCluster = new IncidentCluster();
        dummyCluster.setId(55L);
        dummyCluster.setClusterCode("IND-CLUST-55");
        dummyCluster.setStatus(ClusterStatus.IN_PROGRESS);
        dummyCluster.setSeverity(ClusterSeverity.CRITICAL);
        dummyCluster.setRootCauseHypothesis("Feeder Main Fracture");
        dummyCluster.setRecommendedAction("Pipeline sleeve replacement");
        dummyCluster.setReportCount(4);
        dummyCluster.setStatusNotes("Welding sleeve in place. Water restoration underway.");

        dummyWard = MunicipalWard.builder()
                .wardNumber(85)
                .wardName("Ward 85 - Karol Bagh")
                .officerName("Shri Alok Sharma")
                .officerDesignation("Executive Engineer (EE - Water Central)")
                .contactPhone("+91 98110 23412")
                .contactEmail("ee.water@delhijalboard.nic.in")
                .pincode("110005")
                .municipalBody("Delhi Jal Board (DJB)")
                .build();

        WaterReportRepository reportRepository = (WaterReportRepository) Proxy.newProxyInstance(
                WaterReportRepository.class.getClassLoader(),
                new Class<?>[]{WaterReportRepository.class},
                (proxy, method, args) -> {
                    if ("findByReportCode".equals(method.getName())) {
                        return "IND-H2O-9999".equals(args[0]) ? Optional.of(dummyReport) : Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return null;
                }
        );

        IncidentClusterRepository clusterRepository = (IncidentClusterRepository) Proxy.newProxyInstance(
                IncidentClusterRepository.class.getClassLoader(),
                new Class<?>[]{IncidentClusterRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Long.valueOf(55L).equals(args[0]) ? Optional.of(dummyCluster) : Optional.empty();
                    }
                    return null;
                }
        );

        WardRoutingService wardRoutingService = new WardRoutingService(null) {
            @Override
            public MunicipalWard routeToWard(Double lat, Double lon) {
                return dummyWard;
            }
        };

        liveTrackingService = new LiveTrackingService(reportRepository, clusterRepository, wardRoutingService);
    }

    @Test
    @DisplayName("Should build comprehensive live tracking dossier with 5 sequential milestones")
    void testLiveTrackingDossierGeneration() {
        Optional<LiveGrievanceTrackingDto> opt = liveTrackingService.getLiveTracking("IND-H2O-9999");
        assertTrue(opt.isPresent());

        LiveGrievanceTrackingDto dto = opt.get();
        assertEquals("IND-H2O-9999", dto.getReportCode());
        assertEquals("BURST_PIPE", dto.getIssueType());
        assertEquals(80, dto.getProgressPercentage()); // IN_PROGRESS cluster
        assertEquals("IN_PROGRESS", dto.getCurrentStage());
        assertEquals("Shri Alok Sharma", dto.getOfficerName());
        assertEquals("+91 98110 23412", dto.getOfficerContact());
        assertEquals("IND-CLUST-55", dto.getClusterCode());
        assertEquals("CRITICAL", dto.getClusterSeverity());

        // Verify 5 sequential milestones exist
        assertNotNull(dto.getMilestones());
        assertEquals(5, dto.getMilestones().size());
        assertEquals("SUBMITTED", dto.getMilestones().get(0).getStepKey());
        assertEquals("COMPLETED", dto.getMilestones().get(0).getStatus());

        assertEquals("WARD_TRIAGED", dto.getMilestones().get(1).getStepKey());
        assertEquals("COMPLETED", dto.getMilestones().get(1).getStatus());

        assertEquals("CLUSTERED", dto.getMilestones().get(2).getStepKey());
        assertEquals("COMPLETED", dto.getMilestones().get(2).getStatus());

        assertEquals("DISPATCHED", dto.getMilestones().get(3).getStepKey());
        assertEquals("COMPLETED", dto.getMilestones().get(3).getStatus());

        assertEquals("RESOLVED", dto.getMilestones().get(4).getStepKey());
        assertEquals("CURRENT", dto.getMilestones().get(4).getStatus()); // active on-site
    }

    @Test
    @DisplayName("Should successfully subscribe to SSE live stream")
    void testSubscribeLiveUpdates() {
        SseEmitter emitter = liveTrackingService.subscribeLiveUpdates("IND-H2O-9999");
        assertNotNull(emitter);
    }

    @Test
    @DisplayName("Should record citizen satisfaction feedback")
    void testSubmitCitizenFeedback() {
        LiveGrievanceTrackingDto result = liveTrackingService.submitCitizenFeedback(
                "IND-H2O-9999", 5, "Supply restored in 2 hours. Excellent work DJB!"
        );
        assertNotNull(result);
        assertEquals(5, result.getCitizenRating());
        assertEquals("Supply restored in 2 hours. Excellent work DJB!", result.getCitizenFeedbackComment());
    }
}
