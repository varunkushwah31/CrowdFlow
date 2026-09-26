package com.civic.waterwatch.clustering.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_clusters", indexes = {
        @Index(name = "idx_cluster_status", columnList = "status"),
        @Index(name = "idx_cluster_ward", columnList = "ward_number"),
        @Index(name = "idx_cluster_severity", columnList = "severity")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class IncidentCluster implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_code", unique = true, nullable = false, length = 32)
    private String clusterCode;

    @Column(name = "centroid_lat", nullable = false)
    private Double centroidLat;

    @Column(name = "centroid_lon", nullable = false)
    private Double centroidLon;

    @Column(name = "radius_meters")
    private Double radiusMeters;

    @Column(name = "boundary_geojson", columnDefinition = "TEXT")
    private String boundaryGeoJson;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private ClusterSeverity severity = ClusterSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ClusterStatus status = ClusterStatus.ACTIVE;

    @Column(name = "ward_number")
    private Integer wardNumber;

    @Column(name = "ward_name", length = 128)
    private String wardName;

    @Column(name = "municipal_body", length = 128)
    private String municipalBody = "Delhi Jal Board / Municipal Corporation of Delhi";

    @Column(name = "first_reported_at")
    private LocalDateTime firstReportedAt;

    @Column(name = "last_reported_at")
    private LocalDateTime lastReportedAt;

    @Column(name = "root_cause_hypothesis", length = 512)
    private String rootCauseHypothesis;

    @Column(name = "technical_analysis", length = 2000)
    private String technicalAnalysis;

    @Column(name = "status_notes", length = 1000)
    private String statusNotes;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "recommended_action", length = 1000)
    private String recommendedAction;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "dossier_pdf_path", length = 512)
    private String dossierPdfPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
