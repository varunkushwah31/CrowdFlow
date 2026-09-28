package com.civic.waterwatch.clustering.controller;

import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.clustering.service.SpatialClusteringService;
import com.civic.waterwatch.dispatch.service.CitizenNotificationService;
import com.civic.waterwatch.dispatch.service.MunicipalDispatchService;
import com.civic.waterwatch.exception.ClusterNotFoundException;
import com.civic.waterwatch.exception.InvalidStatusTransitionException;
import com.civic.waterwatch.incident.service.LiveTrackingService;
import com.civic.waterwatch.reporting.service.PdfReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
@Tag(name = "Clustering & Municipal Management (India)", description = "Endpoints for DBSCAN spatial clustering, root cause diagnostics, and municipal PDF dossier generation")
public class IncidentClusterController {

    private final IncidentClusterRepository clusterRepository;
    private final SpatialClusteringService clusteringService;
    private final PdfReportService pdfReportService;
    private final MunicipalDispatchService dispatchService;
    private final CitizenNotificationService citizenNotificationService;
    private final LiveTrackingService liveTrackingService;

    @GetMapping
    @Operation(summary = "List all Indian civic incident clusters")
    public ResponseEntity<List<IncidentCluster>> getAllClusters() {
        return ResponseEntity.ok(clusterRepository.findAllOrderByCreatedAtDesc());
    }

    @GetMapping("/open")
    @Operation(summary = "List all active, open, and escalated clusters")
    @Cacheable(value = "clusters_open", key = "'all'")
    public ResponseEntity<List<IncidentCluster>> getOpenClusters() {
        return ResponseEntity.ok(clusterRepository.findOpenClusters());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a specific cluster")
    public ResponseEntity<IncidentCluster> getClusterById(@PathVariable Long id) {
        return clusterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClusterNotFoundException(id));
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger the spatial DBSCAN clustering algorithm manually across Indian coordinates")
    @CacheEvict(value = "clusters_open", allEntries = true)
    public ResponseEntity<SpatialClusteringService.ClusteringRunSummary> triggerClustering(
            @RequestParam(value = "epsMeters", defaultValue = "150.0") double epsMeters,
            @RequestParam(value = "minPoints", defaultValue = "3") int minPoints,
            @RequestParam(value = "escalationThreshold", defaultValue = "5") int escalationThreshold
    ) {
        if (epsMeters <= 0) {
            throw new IllegalArgumentException("epsMeters must be strictly positive (> 0). Submitted: " + epsMeters);
        }
        if (minPoints < 1) {
            throw new IllegalArgumentException("minPoints must be at least 1. Submitted: " + minPoints);
        }
        if (escalationThreshold < 1) {
            throw new IllegalArgumentException("escalationThreshold must be at least 1. Submitted: " + escalationThreshold);
        }

        SpatialClusteringService.ClusteringRunSummary summary = clusteringService.runClustering(epsMeters, minPoints, escalationThreshold);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate and download the official municipal incident dossier PDF (AMRUT/Jal Board standard)")
    public ResponseEntity<Resource> downloadClusterPdf(@PathVariable Long id) {
        IncidentCluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new ClusterNotFoundException(id));

        byte[] pdfBytes = pdfReportService.generateClusterReport(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Incident-Dossier-" + cluster.getClusterCode() + ".pdf\"")
                .body(new ByteArrayResource(pdfBytes));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Manually trigger municipal dispatch (email and webhook) to Jal Board / Ward Engineer")
    public ResponseEntity<Map<String, Object>> escalateCluster(@PathVariable Long id) {
        IncidentCluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new ClusterNotFoundException(id));

        cluster.setStatus(ClusterStatus.ESCALATED);
        cluster.setEscalatedAt(LocalDateTime.now());
        cluster = clusterRepository.save(cluster);

        dispatchService.dispatchEscalation(cluster);
        liveTrackingService.notifyClusterUpdated(id);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Cluster " + cluster.getClusterCode() + " escalated and dispatched to " + (cluster.getMunicipalBody() != null ? cluster.getMunicipalBody() : "Jal Board") + ".",
                "clusterCode", cluster.getClusterCode()
        ));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update cluster resolution status and automatically notify all contributed Indian citizens")
    public ResponseEntity<IncidentCluster> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        IncidentCluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new ClusterNotFoundException(id));

        String statusStr = body.get("status");
        String notes = body.get("notes");

        if (statusStr != null && !statusStr.isBlank()) {
            ClusterStatus newStatus;
            try {
                newStatus = ClusterStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                String allowed = Arrays.toString(ClusterStatus.values());
                throw new InvalidStatusTransitionException(
                        cluster.getStatus().name(),
                        statusStr,
                        allowed
                );
            }

            cluster.setStatus(newStatus);
            if (notes != null && !notes.isBlank()) {
                cluster.setStatusNotes(notes);
            }
            if (newStatus == ClusterStatus.RESOLVED || newStatus == ClusterStatus.CLOSED) {
                cluster.setResolvedAt(LocalDateTime.now());
            }
            cluster = clusterRepository.save(cluster);

            citizenNotificationService.notifyCitizensOfStatusChange(cluster, newStatus, notes);
            liveTrackingService.notifyClusterUpdated(id);
        }

        return ResponseEntity.ok(cluster);
    }
}
