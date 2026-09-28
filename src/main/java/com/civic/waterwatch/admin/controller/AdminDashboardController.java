package com.civic.waterwatch.admin.controller;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.clustering.service.SpatialClusteringService;
import com.civic.waterwatch.dispatch.model.DispatchLog;
import com.civic.waterwatch.dispatch.repository.DispatchLogRepository;
import com.civic.waterwatch.dispatch.service.CitizenNotificationService;
import com.civic.waterwatch.dispatch.service.MunicipalDispatchService;
import com.civic.waterwatch.exception.ClusterNotFoundException;
import com.civic.waterwatch.exception.InvalidStatusTransitionException;
import com.civic.waterwatch.exception.ReportNotFoundException;
import com.civic.waterwatch.exception.WardNotFoundException;
import com.civic.waterwatch.geo.model.DispatchRoutePlan;
import com.civic.waterwatch.geo.model.EmergencyDepot;
import com.civic.waterwatch.geo.model.WaterTankerUnit;
import com.civic.waterwatch.geo.service.MunicipalCoordinationService;
import com.civic.waterwatch.incident.dto.WaterReportResponseDto;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.incident.service.LiveTrackingService;
import com.civic.waterwatch.redis.RedisGeoSpatialService;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Administrative Control Room Controller.
 * Access is strictly restricted to Government Authorities (ROLE_WARD_OFFICER, ROLE_SUPER_ADMIN)
 * such as Delhi Jal Board & MCD Executive Engineers, Zonal Commissioners, and Admin Team.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('WARD_OFFICER', 'SUPER_ADMIN')")
@Tag(name = "Admin Command & Control (Delhi Jal Board & Municipal Authorities)",
     description = "Privileged operations for triage, cluster escalation, emergency fleet dispatch, and municipal telemetry")
public class AdminDashboardController {

    private final WaterReportRepository reportRepository;
    private final IncidentClusterRepository clusterRepository;
    private final WardRoutingService wardRoutingService;
    private final SpatialClusteringService clusteringService;
    private final MunicipalDispatchService municipalDispatchService;
    private final CitizenNotificationService citizenNotificationService;
    private final LiveTrackingService liveTrackingService;
    private final MunicipalCoordinationService municipalCoordinationService;
    private final DispatchLogRepository dispatchLogRepository;

    @Autowired(required = false)
    private RedisGeoSpatialService redisGeoSpatialService;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Executive Overview KPIs and SLA analytics.
     */
    @GetMapping("/overview")
    @Operation(summary = "Get executive command metrics, grievance counts, cluster stats, and SLA turnaround")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> data = new LinkedHashMap<>();

        List<WaterReport> allReports = reportRepository.findAll();
        List<IncidentCluster> allClusters = clusterRepository.findAll();

        long totalReports = allReports.size();
        long submittedReports = allReports.stream().filter(r -> r.getStatus() == ReportStatus.SUBMITTED).count();
        long clusteredReports = allReports.stream().filter(r -> r.getStatus() == ReportStatus.CLUSTERED).count();
        long escalatedReports = allReports.stream().filter(r -> r.getStatus() == ReportStatus.ESCALATED).count();
        long inProgressReports = allReports.stream().filter(r -> r.getStatus() == ReportStatus.IN_PROGRESS).count();
        long resolvedReports = allReports.stream().filter(r -> r.getStatus() == ReportStatus.RESOLVED).count();
        long rejectedReports = allReports.stream().filter(r -> r.getStatus() == ReportStatus.REJECTED).count();

        long totalClusters = allClusters.size();
        long criticalClusters = allClusters.stream().filter(c -> c.getSeverity() == ClusterSeverity.CRITICAL).count();
        long openClusters = allClusters.stream().filter(c -> c.getStatus() == ClusterStatus.ACTIVE || c.getStatus() == ClusterStatus.UNDER_INVESTIGATION || c.getStatus() == ClusterStatus.ESCALATED).count();
        long resolvedClusters = allClusters.stream().filter(c -> c.getStatus() == ClusterStatus.RESOLVED || c.getStatus() == ClusterStatus.CLOSED).count();

        // Breakdown by issue type
        Map<String, Long> issueTypeCounts = new LinkedHashMap<>();
        for (IssueType it : IssueType.values()) {
            long count = allReports.stream().filter(r -> r.getIssueType() == it).count();
            if (count > 0) {
                issueTypeCounts.put(it.name(), count);
            }
        }

        // Breakdown by ward
        Map<String, Long> wardCounts = new LinkedHashMap<>();
        for (WaterReport r : allReports) {
            String wardKey = r.getWardName() != null ? r.getWardName() : ("Ward " + (r.getWardNumber() != null ? r.getWardNumber() : "Unknown"));
            wardCounts.put(wardKey, wardCounts.getOrDefault(wardKey, 0L) + 1);
        }

        // Average SLA resolution time in hours
        double avgResolutionHours = allReports.stream()
                .filter(r -> r.getStatus() == ReportStatus.RESOLVED && r.getResolvedAt() != null && r.getReportedAt() != null)
                .mapToLong(r -> Duration.between(r.getReportedAt(), r.getResolvedAt()).toMinutes())
                .average()
                .orElse(165.0) / 60.0;

        List<WaterTankerUnit> activeTankers = municipalCoordinationService.getActiveTankers();
        List<EmergencyDepot> depots = municipalCoordinationService.getAllDepots();

        data.put("totalReports", totalReports);
        data.put("submittedReports", submittedReports);
        data.put("clusteredReports", clusteredReports);
        data.put("escalatedReports", escalatedReports);
        data.put("inProgressReports", inProgressReports);
        data.put("resolvedReports", resolvedReports);
        data.put("rejectedReports", rejectedReports);
        data.put("activePendingCount", submittedReports + clusteredReports + escalatedReports + inProgressReports);

        data.put("totalClusters", totalClusters);
        data.put("criticalClusters", criticalClusters);
        data.put("openClusters", openClusters);
        data.put("resolvedClusters", resolvedClusters);

        data.put("activeTankersCount", activeTankers.size());
        data.put("totalDepotsCount", depots.size());
        data.put("averageResolutionHours", Math.round(avgResolutionHours * 10.0) / 10.0);
        data.put("issueTypeCounts", issueTypeCounts);
        data.put("wardCounts", wardCounts);
        data.put("serverTimestamp", LocalDateTime.now());
        data.put("systemStatus", "OPERATIONAL");

        return ResponseEntity.ok(data);
    }

    /**
     * Filterable Grievance Table for Administrative Triage.
     */
    @GetMapping("/reports")
    @Operation(summary = "Get filterable, searchable list of citizen water incident reports")
    public ResponseEntity<List<WaterReportResponseDto>> getReports(
            @RequestParam(value = "status", required = false) String statusStr,
            @RequestParam(value = "issueType", required = false) String issueTypeStr,
            @RequestParam(value = "wardNumber", required = false) Integer wardNumber,
            @RequestParam(value = "search", required = false) String search
    ) {
        List<WaterReport> list = reportRepository.findAllOrderByReportedAtDesc();

        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                ReportStatus targetStatus = ReportStatus.valueOf(statusStr.trim().toUpperCase());
                list = list.stream().filter(r -> r.getStatus() == targetStatus).toList();
            } catch (IllegalArgumentException _) {
                // Ignore invalid status filter
            }
        }

        if (issueTypeStr != null && !issueTypeStr.isBlank() && !"ALL".equalsIgnoreCase(issueTypeStr)) {
            try {
                IssueType targetIssue = IssueType.valueOf(issueTypeStr.trim().toUpperCase());
                list = list.stream().filter(r -> r.getIssueType() == targetIssue).toList();
            } catch (IllegalArgumentException _) {
                // Ignore invalid issue filter
            }
        }

        if (wardNumber != null && wardNumber > 0) {
            list = list.stream().filter(r -> Objects.equals(r.getWardNumber(), wardNumber)).toList();
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            list = list.stream().filter(r ->
                    (r.getReportCode() != null && r.getReportCode().toLowerCase().contains(q)) ||
                    (r.getAddress() != null && r.getAddress().toLowerCase().contains(q)) ||
                    (r.getCitizenName() != null && r.getCitizenName().toLowerCase().contains(q)) ||
                    (r.getCitizenPhone() != null && r.getCitizenPhone().contains(q)) ||
                    (r.getDescription() != null && r.getDescription().toLowerCase().contains(q)) ||
                    (r.getWardName() != null && r.getWardName().toLowerCase().contains(q))
            ).toList();
        }

        List<WaterReportResponseDto> dtos = list.stream().map(WaterReportResponseDto::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Single report detailed inspection.
     */
    @GetMapping("/reports/{id}")
    @Operation(summary = "Get full report details including EXIF hardware camera telemetry")
    public ResponseEntity<WaterReportResponseDto> getReportById(@PathVariable Long id) {
        WaterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        return ResponseEntity.ok(WaterReportResponseDto.fromEntity(report));
    }

    /**
     * Triage status transition for an incident report.
     */
    @PatchMapping("/reports/{id}/status")
    @Operation(summary = "Update report status, record field crew notes, and stream SSE update to citizen")
    @CacheEvict(value = "heatmap", allEntries = true)
    public ResponseEntity<WaterReportResponseDto> updateReportStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        WaterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));

        String statusStr = body.get("status");
        String notes = body.get("notes");

        if (statusStr != null && !statusStr.isBlank()) {
            ReportStatus newStatus;
            try {
                newStatus = ReportStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                String allowed = Arrays.toString(ReportStatus.values());
                throw new InvalidStatusTransitionException(report.getStatus().name(), statusStr, allowed);
            }

            report.setStatus(newStatus);
            if (notes != null && !notes.isBlank()) {
                report.setStatusNotes(notes);
            }
            if (newStatus == ReportStatus.RESOLVED) {
                report.setResolvedAt(LocalDateTime.now());
            }

            report = reportRepository.save(report);
            log.info("Authority updated report [{}] to status: {}", report.getReportCode(), newStatus);

            liveTrackingService.notifyReportUpdated(report.getReportCode());
        }

        return ResponseEntity.ok(WaterReportResponseDto.fromEntity(report));
    }

    /**
     * Delete / Purge report (e.g. spam, test data, or false alarm).
     */
    @DeleteMapping("/reports/{id}")
    @Operation(summary = "Purge invalid or spam report from database and geospatial index")
    @CacheEvict(value = "heatmap", allEntries = true)
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long id) {
        WaterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));

        String reportCode = report.getReportCode();
        reportRepository.delete(report);
        log.warn("Authority purged report [{}] from registry", reportCode);

        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "message", "Report " + reportCode + " successfully purged from civic registry",
                "reportCode", reportCode
        ));
    }

    /**
     * Reassign report to another municipal ward jurisdiction.
     */
    @PostMapping("/reports/{id}/reassign")
    @Operation(summary = "Reassign grievance to a different municipal ward jurisdiction")
    public ResponseEntity<WaterReportResponseDto> reassignReportWard(
            @PathVariable Long id,
            @RequestParam("wardNumber") Integer wardNumber
    ) {
        WaterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));

        MunicipalWard ward = wardRoutingService.findWardByNumber(wardNumber)
                .orElseThrow(() -> new WardNotFoundException(wardNumber));

        report.setWardNumber(ward.getWardNumber());
        report.setWardName(ward.getWardName());
        report.setMunicipalBody(ward.getMunicipalBody());
        report = reportRepository.save(report);

        liveTrackingService.notifyReportUpdated(report.getReportCode());

        return ResponseEntity.ok(WaterReportResponseDto.fromEntity(report));
    }

    /**
     * List all DBSCAN Spatial Clusters.
     */
    @GetMapping("/clusters")
    @Operation(summary = "Get list of all DBSCAN spatial incident clusters")
    public ResponseEntity<List<IncidentCluster>> getClusters() {
        return ResponseEntity.ok(clusterRepository.findAllOrderByCreatedAtDesc());
    }

    /**
     * Manually trigger DBSCAN Spatial Clustering across citizen reports.
     */
    @PostMapping("/clusters/run")
    @Operation(summary = "Execute DBSCAN spatial clustering algorithm with custom distance and min reports")
    @CacheEvict(value = "clusters_open", allEntries = true)
    public ResponseEntity<SpatialClusteringService.ClusteringRunSummary> triggerClustering(
            @RequestParam(value = "epsMeters", defaultValue = "150.0") double epsMeters,
            @RequestParam(value = "minPoints", defaultValue = "3") int minPoints,
            @RequestParam(value = "escalationThreshold", defaultValue = "5") int escalationThreshold
    ) {
        SpatialClusteringService.ClusteringRunSummary summary =
                clusteringService.runClustering(epsMeters, minPoints, escalationThreshold);
        return ResponseEntity.ok(summary);
    }

    /**
     * Escalate cluster to Delhi Jal Board emergency dispatch pipeline.
     */
    @PostMapping("/clusters/{id}/escalate")
    @Operation(summary = "Escalate cluster to Jal Board / MCD dispatch and send automated notices")
    public ResponseEntity<Map<String, Object>> escalateCluster(@PathVariable Long id) {
        IncidentCluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new ClusterNotFoundException(id));

        cluster.setStatus(ClusterStatus.ESCALATED);
        cluster.setEscalatedAt(LocalDateTime.now());
        cluster = clusterRepository.save(cluster);

        municipalDispatchService.dispatchEscalation(cluster);
        liveTrackingService.notifyClusterUpdated(id);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Cluster " + cluster.getClusterCode() + " escalated and dispatched to " + cluster.getMunicipalBody(),
                "clusterCode", cluster.getClusterCode()
        ));
    }

    /**
     * Update cluster status and resolution notes.
     */
    @PatchMapping("/clusters/{id}/status")
    @Operation(summary = "Update cluster status, notify affected citizens, and record notes")
    public ResponseEntity<IncidentCluster> updateClusterStatus(
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
                throw new InvalidStatusTransitionException(cluster.getStatus().name(), statusStr, allowed);
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

    /**
     * Fleet telemetry and tanker management.
     */
    @GetMapping("/fleet/status")
    @Operation(summary = "Get live GPS positions of water tankers and emergency depot bases")
    public ResponseEntity<Map<String, Object>> getFleetStatus() {
        return ResponseEntity.ok(Map.of(
                "depots", municipalCoordinationService.getAllDepots(),
                "tankers", municipalCoordinationService.getActiveTankers()
        ));
    }

    /**
     * Dispatch emergency water tanker or repair unit.
     */
    @PostMapping("/fleet/dispatch")
    @Operation(summary = "Issue rapid dispatch order for water tanker or pipeline repair van")
    public ResponseEntity<DispatchRoutePlan> dispatchUnit(
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon,
            @RequestParam(value = "desc", defaultValue = "EMERGENCY_DISPATCH") String desc
    ) {
        DispatchRoutePlan plan = municipalCoordinationService.coordinateDispatch(lat, lon, desc);
        log.info("Authority dispatched {} from depot {} to [{}, {}]. ETA: {} mins",
                plan.getRecommendedVehicleType(), plan.getAssignedDepot().getName(), lat, lon, plan.getEstimatedMinutes());
        return ResponseEntity.ok(plan);
    }

    /**
     * Municipal Dispatch Audit Logs.
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "Audit trail of all automated email, webhook, and SMS dispatches")
    public ResponseEntity<List<DispatchLog>> getAuditLogs() {
        return ResponseEntity.ok(dispatchLogRepository.findAllByOrderByDispatchedAtDesc());
    }

    /**
     * Redis Cache Eviction.
     */
    @PostMapping("/cache/clear")
    @Operation(summary = "Evict specific Redis cache partition or all partitions")
    public ResponseEntity<Map<String, Object>> clearCache(
            @RequestParam(value = "cacheName", defaultValue = "all") String cacheName
    ) {
        List<String> evicted = new ArrayList<>();
        if (cacheManager != null) {
            if ("all".equalsIgnoreCase(cacheName)) {
                for (String name : cacheManager.getCacheNames()) {
                    Cache c = cacheManager.getCache(name);
                    if (c != null) {
                        c.clear();
                        evicted.add(name);
                    }
                }
            } else {
                Cache c = cacheManager.getCache(cacheName);
                if (c != null) {
                    c.clear();
                    evicted.add(cacheName);
                }
            }
        }
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Evicted caches: " + evicted,
                "evictedCaches", evicted,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * System Diagnostics & Server Health.
     */
    @GetMapping("/diagnostics")
    @Operation(summary = "System diagnostics: JVM memory, Redis connectivity, thread count, and uptime")
    public ResponseEntity<Map<String, Object>> getDiagnostics() {
        Map<String, Object> diag = new LinkedHashMap<>();

        // JVM memory
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        diag.put("jvmUsedMemoryMb", usedMemory / (1024 * 1024));
        diag.put("jvmTotalMemoryMb", totalMemory / (1024 * 1024));
        diag.put("jvmMaxMemoryMb", maxMemory / (1024 * 1024));
        diag.put("availableProcessors", runtime.availableProcessors());
        diag.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        // Redis connectivity
        String redisPing = "UNAVAILABLE";
        if (redisConnectionFactory != null) {
            try (RedisConnection conn = redisConnectionFactory.getConnection()) {
                redisPing = conn.ping();
            } catch (Exception e) {
                redisPing = "FAILED: " + e.getMessage();
            }
        }
        diag.put("redisPing", redisPing);
        diag.put("redisHealthy", "PONG".equalsIgnoreCase(redisPing));

        // PostGIS / Database count
        try {
            long count = reportRepository.count();
            diag.put("databaseConnected", true);
            diag.put("persistedReportsCount", count);
        } catch (Exception e) {
            diag.put("databaseConnected", false);
            diag.put("databaseError", e.getMessage());
        }

        diag.put("serverTime", LocalDateTime.now());
        return ResponseEntity.ok(diag);
    }
}
