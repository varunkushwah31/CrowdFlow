package com.civic.waterwatch.dispatch.controller;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.dispatch.model.DispatchLog;
import com.civic.waterwatch.dispatch.repository.DispatchLogRepository;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Municipal Portal & Administration (India)", description = "Indian municipal ward routing, dispatch audit trails, and system health")
public class MunicipalPortalController {

    private final WardRoutingService wardRoutingService;
    private final DispatchLogRepository dispatchLogRepository;
    private final WaterReportRepository reportRepository;
    private final IncidentClusterRepository clusterRepository;

    @GetMapping("/wards")
    @Operation(summary = "Get directory of all Indian municipal wards and emergency routing endpoints")
    public ResponseEntity<List<MunicipalWard>> getWards() {
        return ResponseEntity.ok(wardRoutingService.getAllWards());
    }

    @GetMapping("/dispatch/logs")
    @Operation(summary = "Get audit trail of all automated dispatches, Jal Board emails, webhooks, and citizen SMS notices")
    public ResponseEntity<List<DispatchLog>> getDispatchLogs() {
        return ResponseEntity.ok(dispatchLogRepository.findAllByOrderByDispatchedAtDesc());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform metrics: total citizen reports, clustered incidents, critical alerts, and resolutions")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        long totalReports = reportRepository.count();
        long unclusteredReports = reportRepository.countByStatus(ReportStatus.SUBMITTED);
        long totalClusters = clusterRepository.count();
        long criticalClusters = clusterRepository.countBySeverity(ClusterSeverity.CRITICAL);
        long escalatedClusters = clusterRepository.countByStatus(ClusterStatus.ESCALATED);
        long resolvedClusters = clusterRepository.countByStatus(ClusterStatus.RESOLVED);

        return ResponseEntity.ok(Map.of(
                "totalReports", totalReports,
                "unclusteredReports", unclusteredReports,
                "totalClusters", totalClusters,
                "criticalClusters", criticalClusters,
                "escalatedClusters", escalatedClusters,
                "resolvedClusters", resolvedClusters,
                "helpline", "1916 (Delhi Jal Board Toll-Free Helpline)",
                "country", "India"
        ));
    }

    @PostMapping("/municipal/mock-webhook/{wardId}")
    @Operation(summary = "Simulated endpoint mimicking Municipal Grievance intake server")
    public ResponseEntity<Map<String, Object>> mockMunicipalWebhookReceiver(
            @PathVariable String wardId,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("INDIAN MUNICIPAL GRIEVANCE PORTAL: Acknowledged automated incident dossier for Ward [{}]! Payload: {}",
                wardId, payload);
        return ResponseEntity.ok(Map.of(
                "receptionStatus", "ACKNOWLEDGED",
                "grievanceTicketNumber", "DJB-GRIEVANCE-" + System.currentTimeMillis() % 100000,
                "ward", wardId,
                "actionAssignedTo", "Executive Engineer (EE - Water)",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
