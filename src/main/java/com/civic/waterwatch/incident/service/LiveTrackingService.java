package com.civic.waterwatch.incident.service;

import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto;
import com.civic.waterwatch.incident.dto.TrackingMilestone;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service orchestrating live, real-time grievance tracking for Indian citizens.
 * Supports:
 * 1. Live SSE (Server-Sent Events) streaming directly to citizen browser / mobile.
 * 2. 5-Stage municipal resolution lifecycle timeline.
 * 3. Associated spatial DBSCAN cluster correlation context.
 * 4. Nodal engineer direct dispatch information and citizen feedback loop.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LiveTrackingService {

    private final WaterReportRepository reportRepository;
    private final IncidentClusterRepository clusterRepository;
    private final WardRoutingService wardRoutingService;

    // Real-time SSE subscriber registry: reportCode -> List of active SseEmitters
    private final Map<String, List<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    /**
     * Builds the complete real-time tracking dossier for a citizen's grievance.
     */
    @org.springframework.cache.annotation.Cacheable(value = "live_tracking", key = "#reportCode", unless = "#result == null || !#result.isPresent()")
    public Optional<LiveGrievanceTrackingDto> getLiveTracking(String reportCode) {
        return reportRepository.findByReportCode(reportCode).map(this::buildTrackingDto);
    }

    /**
     * Subscribes a citizen's client to real-time Server-Sent Events (SSE).
     * Automatically pushes initial state upon connection and streams field updates live.
     */
    public SseEmitter subscribeLiveUpdates(String reportCode) {
        // 5-minute SSE connection timeout
        SseEmitter emitter = new SseEmitter(300_000L);

        activeEmitters.computeIfAbsent(reportCode, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(reportCode, emitter));
        emitter.onTimeout(() -> removeEmitter(reportCode, emitter));
        emitter.onError(e -> removeEmitter(reportCode, emitter));

        // Push initial live tracking snapshot immediately upon connection
        getLiveTracking(reportCode).ifPresent(dto -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("grievance-status")
                        .data(dto));
            } catch (IOException e) {
                log.warn("Failed to send initial SSE event to {}: {}", reportCode, e.getMessage());
            }
        });

        log.info("Citizen subscribed to LIVE stream for grievance: {}", reportCode);
        return emitter;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.civic.waterwatch.redis.RedisPubSubService redisPubSubService;

    public void setRedisPubSubService(com.civic.waterwatch.redis.RedisPubSubService redisPubSubService) {
        this.redisPubSubService = redisPubSubService;
    }

    /**
     * Broadcasts live update event to all connected citizens tracking this report.
     * Propagates locally to connected SSE emitters and publishes across cluster via Redis Pub/Sub.
     */
    @org.springframework.cache.annotation.CacheEvict(value = "live_tracking", key = "#reportCode")
    public void notifyReportUpdated(String reportCode) {
        broadcastLocalSse(reportCode);
        if (redisPubSubService != null) {
            redisPubSubService.publishIncidentUpdate(reportCode, null, "REPORT_UPDATED");
        }
    }

    /**
     * Directly pushes SSE update to emitters connected to this JVM instance.
     */
    public void broadcastLocalSse(String reportCode) {
        List<SseEmitter> emitters = activeEmitters.get(reportCode);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        getLiveTracking(reportCode).ifPresent(dto -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("grievance-status")
                            .data(dto));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        });
    }

    /**
     * Broadcasts live updates to all citizen grievances grouped into a municipal cluster.
     */
    public void notifyClusterUpdated(Long clusterId) {
        List<WaterReport> reports = reportRepository.findByClusterId(clusterId);
        for (WaterReport report : reports) {
            notifyReportUpdated(report.getReportCode());
        }
    }

    /**
     * Records citizen satisfaction feedback (1 to 5 stars + review comment) once resolved.
     */
    @Transactional
    public LiveGrievanceTrackingDto submitCitizenFeedback(String reportCode, int rating, String comments) {
        WaterReport report = reportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + reportCode));

        report.setCitizenRating(Math.clamp(rating, 1, 5));
        report.setCitizenFeedbackComment(comments);
        report.setFeedbackSubmittedAt(LocalDateTime.now());
        report = reportRepository.save(report);

        log.info("Citizen feedback logged for {}: {} stars - '{}'", reportCode, rating, comments);

        LiveGrievanceTrackingDto dto = buildTrackingDto(report);
        notifyReportUpdated(reportCode);
        return dto;
    }

    private void removeEmitter(String reportCode, SseEmitter emitter) {
        List<SseEmitter> emitters = activeEmitters.get(reportCode);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeEmitters.remove(reportCode);
            }
        }
    }

    private LiveGrievanceTrackingDto buildTrackingDto(WaterReport report) {
        MunicipalWard ward = null;
        if (report.getLatitude() != null && report.getLongitude() != null) {
            ward = wardRoutingService.routeToWard(report.getLatitude(), report.getLongitude());
        }

        IncidentCluster cluster = null;
        if (report.getClusterId() != null) {
            cluster = clusterRepository.findById(report.getClusterId()).orElse(null);
        }

        // Determine current stage & progress percentage (20%, 40%, 60%, 80%, 100%)
        String currentStage;
        int progressPercentage;
        String statusDesc;

        if (report.getStatus() == ReportStatus.RESOLVED || (cluster != null && cluster.getStatus() == ClusterStatus.RESOLVED)) {
            currentStage = "RESOLVED";
            progressPercentage = 100;
            statusDesc = "Pipeline Repaired & Water Supply Restored to Normal Pressure";
        } else if (cluster != null && cluster.getStatus() == ClusterStatus.IN_PROGRESS) {
            currentStage = "IN_PROGRESS";
            progressPercentage = 80;
            statusDesc = "Delhi Jal Board Repair Unit On-Site (Welding / Valve Replacement)";
        } else if (cluster != null && (cluster.getStatus() == ClusterStatus.ESCALATED || cluster.getStatus() == ClusterStatus.UNDER_INVESTIGATION)) {
            currentStage = "DISPATCHED";
            progressPercentage = 60;
            statusDesc = "Emergency Work Order Dispatched to Executive Engineer";
        } else if (cluster != null || report.getStatus() == ReportStatus.CLUSTERED) {
            currentStage = "CLUSTERED";
            progressPercentage = 40;
            statusDesc = "Correlated into Incident Hotspot with Nearby Citizen Reports";
        } else if (ward != null) {
            currentStage = "WARD_TRIAGED";
            progressPercentage = 20;
            statusDesc = "Verified via EXIF GPS & Routed to " + ward.getWardName();
        } else {
            currentStage = "SUBMITTED";
            progressPercentage = 10;
            statusDesc = "Citizen Grievance Logged in Central Intake Queue";
        }

        // Determine ETA based on issue severity
        String estimatedEta = switch (report.getIssueType()) {
            case BURST_PIPE -> "Within 2 - 4 Hours (Emergency Feeder Main Protocol)";
            case CONTAMINATION -> "Within 4 - 6 Hours (Water Quality & Sewage Isolation Team)";
            case SEVERE_WATERLOGGING, DRAINAGE_OVERFLOW -> "Within 4 Hours (MCD Super Sucker Drainage Unit)";
            case LOW_PRESSURE, WATER_SCARCITY -> "Within 12 Hours (Booster & Tanker Mobilization)";
            default -> "Within 24 Hours (Standard Municipal Maintenance)";
        };

        // Construct 5 Sequential Milestones
        List<TrackingMilestone> milestones = new ArrayList<>();

        // Stage 1: Grievance Intake
        milestones.add(TrackingMilestone.builder()
                .stepKey("SUBMITTED")
                .title("Grievance Intake & EXIF GPS Verification")
                .description(String.format("Photo evidence uploaded with hardware EXIF coordinates (%.4f N, %.4f E). PII stripped.",
                        report.getLatitude() != null ? report.getLatitude() : 28.6320,
                        report.getLongitude() != null ? report.getLongitude() : 77.2105))
                .icon("fa-camera")
                .status("COMPLETED")
                .timestamp(report.getReportedAt())
                .actor("Citizen Mobile Client (India)")
                .build());

        // Stage 2: Spatial Ward Triage
        boolean wardPassed = progressPercentage >= 20;
        milestones.add(TrackingMilestone.builder()
                .stepKey("WARD_TRIAGED")
                .title("Delhi Municipal Ward Boundary Routing")
                .description(ward != null
                        ? String.format("Mapped into %s (PIN: %s). Nodal jurisdiction: %s",
                        ward.getWardName(), ward.getPincode(), ward.getMunicipalBody())
                        : "Mapped to Central Delhi Jal Board Municipal Division")
                .icon("fa-building-shield")
                .status(wardPassed ? "COMPLETED" : "PENDING")
                .timestamp(report.getReportedAt() != null ? report.getReportedAt().plusMinutes(1) : LocalDateTime.now())
                .actor(ward != null ? ward.getOfficerDesignation() : "Spatial Containment Engine")
                .build());

        // Stage 3: DBSCAN Spatial Hotspot Correlation
        boolean clusterPassed = progressPercentage >= 40;
        milestones.add(TrackingMilestone.builder()
                .stepKey("CLUSTERED")
                .title("DBSCAN Spatial Hotspot Correlation")
                .description(cluster != null
                        ? String.format("Grouped into Cluster %s with %d neighbor reports within 150m. Root cause diagnosed as: %s",
                        cluster.getClusterCode(), cluster.getReportCount(), cluster.getRootCauseHypothesis())
                        : "Monitoring neighborhood coordinates for concurrent complaint clustering.")
                .icon("fa-circle-nodes")
                .status(clusterPassed ? "COMPLETED" : (progressPercentage == 20 ? "CURRENT" : "PENDING"))
                .timestamp(cluster != null ? cluster.getCreatedAt() : null)
                .actor("Geodesic DBSCAN Spatial Engine")
                .build());

        // Stage 4: Work Order Dispatch
        boolean dispatchPassed = progressPercentage >= 60;
        milestones.add(TrackingMilestone.builder()
                .stepKey("DISPATCHED")
                .title("Work Order & Field Engineering Dispatch")
                .description(cluster != null && cluster.getEscalatedAt() != null
                        ? String.format("Official dossier dispatched to %s (%s). Crew scheduled: %s",
                        ward != null ? ward.getOfficerName() : "Nodal Engineer",
                        ward != null ? ward.getContactPhone() : "+91 98110 23412",
                        estimatedEta)
                        : "Awaiting field crew dispatch trigger.")
                .icon("fa-truck-fast")
                .status(dispatchPassed ? (progressPercentage == 60 ? "CURRENT" : "COMPLETED") : "PENDING")
                .timestamp(cluster != null ? cluster.getEscalatedAt() : null)
                .actor(ward != null ? ward.getOfficerName() + " (" + ward.getOfficerDesignation() + ")" : "Municipal Dispatcher")
                .build());

        // Stage 5: Restoration & Closure
        boolean resolvedPassed = progressPercentage == 100;
        milestones.add(TrackingMilestone.builder()
                .stepKey("RESOLVED")
                .title("On-Site Restoration & Quality Verification")
                .description(resolvedPassed
                        ? (report.getStatusNotes() != null
                        ? report.getStatusNotes()
                        : "Physical repairs verified on site. Water pressure normalized and lines sanitized.")
                        : "Field repair team actively addressing the infrastructure breakdown.")
                .icon("fa-circle-check")
                .status(resolvedPassed ? "COMPLETED" : (progressPercentage >= 80 ? "CURRENT" : "PENDING"))
                .timestamp(report.getResolvedAt())
                .actor("Delhi Jal Board Field Engineering Crew")
                .build());

        return LiveGrievanceTrackingDto.builder()
                .reportCode(report.getReportCode())
                .issueType(report.getIssueType() != null ? report.getIssueType().name() : "OTHER")
                .issueTypeName(report.getIssueType() != null ? report.getIssueType().getDisplayName() : "Water Hazard")
                .category(report.getIssueType() != null ? report.getIssueType().getCategory() : "CIVIC")
                .description(report.getDescription())
                .imageUrl(report.getImageUrl())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .address(report.getAddress())
                .neighborhood(report.getNeighborhood())
                .wardNumber(report.getWardNumber())
                .wardName(report.getWardName())
                .municipalBody(report.getMunicipalBody() != null ? report.getMunicipalBody() : "Delhi Jal Board (DJB)")
                .reportedAt(report.getReportedAt())
                .currentStage(currentStage)
                .progressPercentage(progressPercentage)
                .statusDescription(statusDesc)
                .latestFieldNotes(report.getStatusNotes() != null ? report.getStatusNotes() : (cluster != null ? cluster.getStatusNotes() : null))
                .lastUpdatedAt(LocalDateTime.now())
                .officerName(ward != null ? ward.getOfficerName() : "Shri Alok Sharma")
                .officerRole(ward != null ? ward.getOfficerDesignation() : "Executive Engineer (Water)")
                .officerContact(ward != null ? ward.getContactPhone() : "+91 98110 23412")
                .officerEmail(ward != null ? ward.getContactEmail() : "ee.water@delhijalboard.nic.in")
                .helpline("1916 (Delhi Jal Board Toll-Free)")
                .estimatedResolutionTime(estimatedEta)
                .clusterId(cluster != null ? cluster.getId() : null)
                .clusterCode(cluster != null ? cluster.getClusterCode() : null)
                .clusterSeverity(cluster != null && cluster.getSeverity() != null ? cluster.getSeverity().name() : null)
                .rootCauseSummary(cluster != null ? cluster.getRootCauseHypothesis() : null)
                .recommendedRemedy(cluster != null ? cluster.getRecommendedAction() : null)
                .neighborReportCount(cluster != null ? cluster.getReportCount() : 1)
                .pdfDossierUrl(cluster != null ? "/api/clusters/" + cluster.getId() + "/pdf" : null)
                .milestones(milestones)
                .citizenRating(report.getCitizenRating())
                .citizenFeedbackComment(report.getCitizenFeedbackComment())
                .feedbackSubmittedAt(report.getFeedbackSubmittedAt())
                .build();
    }
}
