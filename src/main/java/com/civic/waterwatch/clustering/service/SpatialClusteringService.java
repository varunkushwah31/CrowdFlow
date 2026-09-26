package com.civic.waterwatch.clustering.service;

import com.civic.waterwatch.clustering.event.ClusterThresholdReachedEvent;
import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.model.RootCauseAnalysis;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpatialClusteringService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final WaterReportRepository reportRepository;
    private final IncidentClusterRepository clusterRepository;
    private final RootCauseCorrelationEngine correlationEngine;
    private final WardRoutingService wardRoutingService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${waterwatch.clustering.eps-meters:150.0}")
    private double defaultEpsMeters;

    @Value("${waterwatch.clustering.min-points:3}")
    private int defaultMinPoints;

    @Value("${waterwatch.clustering.escalation-threshold:5}")
    private int defaultEscalationThreshold;

    @Getter
    @AllArgsConstructor
    public static class ClusteringRunSummary {
        private final int totalCandidates;
        private final int clustersFound;
        private final int reportsClustered;
        private final int unclusteredNoise;
        private final int escalationsTriggered;
    }

    @Transactional
    public ClusteringRunSummary runClustering() {
        return runClustering(defaultEpsMeters, defaultMinPoints, defaultEscalationThreshold);
    }

    @Transactional
    public ClusteringRunSummary runClustering(double epsMeters, int minPoints, int escalationThreshold) {
        log.info("Executing spatial clustering DBSCAN across Indian municipal precincts (eps: {}m, minPoints: {}, escalationThreshold: {})...",
                epsMeters, minPoints, escalationThreshold);

        List<WaterReport> candidates = reportRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReportStatus.RESOLVED && r.getStatus() != ReportStatus.REJECTED)
                .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
                .toList();

        if (candidates.isEmpty()) {
            log.info("No candidates for clustering.");
            return new ClusteringRunSummary(0, 0, 0, 0, 0);
        }

        List<List<WaterReport>> clusters = dbscan(candidates, epsMeters, minPoints);

        int totalClusteredReports = 0;
        int escalationsCount = 0;

        for (List<WaterReport> clusterPoints : clusters) {
            totalClusteredReports += clusterPoints.size();
            IncidentCluster cluster = persistOrUpdateCluster(clusterPoints, escalationThreshold);
            if (cluster != null && cluster.getStatus() == ClusterStatus.ESCALATED && cluster.getEscalatedAt() != null) {
                escalationsCount++;
            }
        }

        int noiseCount = candidates.size() - totalClusteredReports;
        log.info("Spatial clustering finished: {} clusters formed from {} reports ({} unclustered noise points).",
                clusters.size(), totalClusteredReports, noiseCount);

        return new ClusteringRunSummary(candidates.size(), clusters.size(), totalClusteredReports, noiseCount, escalationsCount);
    }

    private List<List<WaterReport>> dbscan(List<WaterReport> points, double epsMeters, int minPts) {
        List<List<WaterReport>> clusters = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> clustered = new HashSet<>();

        for (WaterReport point : points) {
            if (visited.contains(point.getId())) {
                continue;
            }
            visited.add(point.getId());

            List<WaterReport> neighbors = getNeighbors(point, points, epsMeters);
            if (neighbors.size() < minPts) {
                continue;
            }

            List<WaterReport> cluster = new ArrayList<>();
            cluster.add(point);
            clustered.add(point.getId());

            Queue<WaterReport> queue = new ArrayDeque<>(neighbors);
            while (!queue.isEmpty()) {
                WaterReport current = queue.poll();

                if (!visited.contains(current.getId())) {
                    visited.add(current.getId());
                    List<WaterReport> currentNeighbors = getNeighbors(current, points, epsMeters);
                    if (currentNeighbors.size() >= minPts) {
                        queue.addAll(currentNeighbors);
                    }
                }

                if (!clustered.contains(current.getId())) {
                    cluster.add(current);
                    clustered.add(current.getId());
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    private List<WaterReport> getNeighbors(WaterReport center, List<WaterReport> points, double epsMeters) {
        List<WaterReport> neighbors = new ArrayList<>();
        for (WaterReport p : points) {
            double dist = haversineDistanceMeters(center.getLatitude(), center.getLongitude(),
                    p.getLatitude(), p.getLongitude());
            if (dist <= epsMeters) {
                neighbors.add(p);
            }
        }
        return neighbors;
    }

    public static double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private IncidentCluster persistOrUpdateCluster(List<WaterReport> points, int escalationThreshold) {
        if (points.isEmpty()) return null;

        double sumLat = 0.0;
        double sumLon = 0.0;
        LocalDateTime earliest = points.getFirst().getReportedAt();
        LocalDateTime latest = points.getFirst().getReportedAt();

        for (WaterReport r : points) {
            sumLat += r.getLatitude();
            sumLon += r.getLongitude();
            if (r.getReportedAt() != null) {
                if (earliest == null || r.getReportedAt().isBefore(earliest)) earliest = r.getReportedAt();
                if (latest == null || r.getReportedAt().isAfter(latest)) latest = r.getReportedAt();
            }
        }

        double centroidLat = sumLat / points.size();
        double centroidLon = sumLon / points.size();

        double maxRadius = 0.0;
        for (WaterReport r : points) {
            double dist = haversineDistanceMeters(centroidLat, centroidLon, r.getLatitude(), r.getLongitude());
            if (dist > maxRadius) {
                maxRadius = dist;
            }
        }

        String boundaryGeoJson = computeConvexHullGeoJson(points, centroidLat, centroidLon, Math.max(maxRadius, 30.0));
        RootCauseAnalysis analysis = correlationEngine.analyze(points);

        Long existingClusterId = points.stream()
                .map(WaterReport::getClusterId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        IncidentCluster cluster;
        if (existingClusterId != null && clusterRepository.existsById(existingClusterId)) {
            cluster = clusterRepository.findById(existingClusterId).orElse(new IncidentCluster());
        } else {
            cluster = new IncidentCluster();
            cluster.setClusterCode("IND-CLUST-" + (1000 + (long) (Math.random() * 9000)));
            cluster.setStatus(ClusterStatus.ACTIVE);
        }

        cluster.setCentroidLat(centroidLat);
        cluster.setCentroidLon(centroidLon);
        cluster.setRadiusMeters(Math.round(maxRadius * 10.0) / 10.0);
        cluster.setBoundaryGeoJson(boundaryGeoJson);
        cluster.setReportCount(points.size());
        cluster.setSeverity(analysis.getSuggestedSeverity());
        cluster.setFirstReportedAt(earliest);
        cluster.setLastReportedAt(latest);
        cluster.setRootCauseHypothesis(analysis.getPrimaryHypothesis());
        cluster.setTechnicalAnalysis(analysis.getTechnicalSummary());
        cluster.setConfidenceScore(analysis.getConfidenceScore());
        cluster.setRecommendedAction(analysis.getRecommendedAction());

        MunicipalWard ward = wardRoutingService.routeToWard(centroidLat, centroidLon);
        if (ward != null) {
            cluster.setWardNumber(ward.getWardNumber());
            cluster.setWardName(ward.getWardName());
            cluster.setMunicipalBody(ward.getMunicipalBody());
        }

        cluster = clusterRepository.save(cluster);

        for (WaterReport r : points) {
            r.setClusterId(cluster.getId());
            if (r.getStatus() == ReportStatus.SUBMITTED) {
                r.setStatus(ReportStatus.CLUSTERED);
            }
            if (ward != null && r.getWardNumber() == null) {
                r.setWardNumber(ward.getWardNumber());
                r.setWardName(ward.getWardName());
                r.setMunicipalBody(ward.getMunicipalBody());
            }
            reportRepository.save(r);
        }

        boolean thresholdReached = (cluster.getReportCount() >= escalationThreshold || cluster.getSeverity() == ClusterSeverity.CRITICAL);
        boolean notYetEscalated = (cluster.getStatus() == ClusterStatus.ACTIVE || cluster.getEscalatedAt() == null);

        if (thresholdReached && notYetEscalated) {
            cluster.setStatus(ClusterStatus.ESCALATED);
            cluster.setEscalatedAt(LocalDateTime.now());
            cluster = clusterRepository.save(cluster);

            for (WaterReport r : points) {
                if (r.getStatus() == ReportStatus.CLUSTERED) {
                    r.setStatus(ReportStatus.ESCALATED);
                    reportRepository.save(r);
                }
            }

            log.info("Threshold reached for Indian Cluster {}: Publishing automated municipal escalation event!", cluster.getClusterCode());
            eventPublisher.publishEvent(new ClusterThresholdReachedEvent(this, cluster,
                    "Cluster reached " + cluster.getReportCount() + " verified citizen grievances with severity " + cluster.getSeverity()));
        }

        return cluster;
    }

    private String computeConvexHullGeoJson(List<WaterReport> points, double centerLat, double centerLon, double bufferMeters) {
        try {
            if (points.size() >= 3) {
                Coordinate[] coords = new Coordinate[points.size()];
                for (int i = 0; i < points.size(); i++) {
                    coords[i] = new Coordinate(points.get(i).getLongitude(), points.get(i).getLatitude());
                }

                Geometry hull = new ConvexHull(coords, GEOMETRY_FACTORY).getConvexHull();
                if (hull instanceof Polygon polygon) {
                    return polygonToGeoJson(polygon);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to compute convex hull polygon: {}", e.getMessage());
        }

        return generateCirclePolygonGeoJson(centerLat, centerLon, bufferMeters);
    }

    private String polygonToGeoJson(Polygon polygon) {
        LineString shell = polygon.getExteriorRing();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Polygon\",\"coordinates\":[[");
        for (int i = 0; i < shell.getNumPoints(); i++) {
            Coordinate c = shell.getCoordinateN(i);
            if (i > 0) sb.append(",");
            sb.append("[").append(c.x).append(",").append(c.y).append("]");
        }
        sb.append("]]}");
        return sb.toString();
    }

    private String generateCirclePolygonGeoJson(double centerLat, double centerLon, double radiusMeters) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Polygon\",\"coordinates\":[[");

        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            double dLat = (radiusMeters / 111320.0) * Math.cos(angle);
            double dLon = (radiusMeters / (111320.0 * Math.cos(Math.toRadians(centerLat)))) * Math.sin(angle);
            double pLat = centerLat + dLat;
            double pLon = centerLon + dLon;

            if (i > 0) sb.append(",");
            sb.append("[").append(pLon).append(",").append(pLat).append("]");
        }
        sb.append("]]}");
        return sb.toString();
    }
}
