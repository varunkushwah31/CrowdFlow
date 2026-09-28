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

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    @GetMapping("/wards/{wardNumber}")
    @Operation(summary = "Get specific municipal ward by official ward number (e.g. Ward 85 Karol Bagh)")
    public ResponseEntity<MunicipalWard> getWardByNumber(@PathVariable Integer wardNumber) {
        return wardRoutingService.findWardByNumber(wardNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wards/geojson")
    @Operation(summary = "Get official GeoJSON FeatureCollection of all Delhi municipal ward boundary polygons")
    public ResponseEntity<Map<String, Object>> getWardsGeoJson() {
        List<MunicipalWard> wards = wardRoutingService.getAllWards();

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();

        for (MunicipalWard w : wards) {
            if (w.getMinLat() != null && w.getMaxLat() != null && w.getMinLon() != null && w.getMaxLon() != null) {
                Map<String, Object> feature = new LinkedHashMap<>();
                feature.put("type", "Feature");
                feature.put("id", w.getWardNumber());

                Map<String, Object> geometry = new LinkedHashMap<>();
                geometry.put("type", "Polygon");

                // GeoJSON uses [longitude, latitude] coordinates in counter-clockwise order
                List<List<Double>> ring = List.of(
                        List.of(w.getMinLon(), w.getMinLat()),
                        List.of(w.getMaxLon(), w.getMinLat()),
                        List.of(w.getMaxLon(), w.getMaxLat()),
                        List.of(w.getMinLon(), w.getMaxLat()),
                        List.of(w.getMinLon(), w.getMinLat())
                );
                geometry.put("coordinates", List.of(ring));
                feature.put("geometry", geometry);

                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("wardNumber", w.getWardNumber());
                properties.put("wardName", w.getWardName());
                properties.put("zoneName", w.getZoneName());
                properties.put("municipalBody", w.getMunicipalBody());
                properties.put("state", w.getState());
                properties.put("pincode", w.getPincode());
                properties.put("officerName", w.getOfficerName());
                properties.put("officerDesignation", w.getOfficerDesignation());
                properties.put("contactPhone", w.getContactPhone());
                properties.put("contactEmail", w.getContactEmail());
                properties.put("emergencyHotline", w.getEmergencyHotline());
                properties.put("centerLat", w.getCenterLat());
                properties.put("centerLon", w.getCenterLon());

                feature.put("properties", properties);
                features.add(feature);
            }
        }

        featureCollection.put("features", features);
        return ResponseEntity.ok(featureCollection);
    }

    @GetMapping("/wards/lookup")
    @Operation(summary = "Resolve GPS coordinates to matching municipal ward with containment status and distance")
    public ResponseEntity<Map<String, Object>> routeCoordinate(
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon
    ) {
        MunicipalWard ward = wardRoutingService.routeToWard(lat, lon);
        if (ward == null) {
            return ResponseEntity.notFound().build();
        }

        boolean directContainment = ward.contains(lat, lon);
        double distanceMeters = ward.distanceToCenter(lat, lon);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("latitude", lat);
        response.put("longitude", lon);
        response.put("directlyContained", directContainment);
        response.put("distanceToCentroidMeters", Math.round(distanceMeters * 10.0) / 10.0);
        response.put("ward", ward);

        return ResponseEntity.ok(response);
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
