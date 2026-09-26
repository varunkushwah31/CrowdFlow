package com.civic.waterwatch.incident.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDateTime;

@Entity
@Table(name = "water_reports", indexes = {
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_cluster", columnList = "cluster_id"),
        @Index(name = "idx_report_reported_at", columnList = "reported_at"),
        @Index(name = "idx_report_ward", columnList = "ward_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"location"})
public class WaterReport {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", unique = true, nullable = false, length = 32)
    private String reportCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 32)
    private IssueType issueType;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "media_type", length = 32)
    private String mediaType = "IMAGE";

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @JsonIgnore
    @Column(name = "location")
    private Point location;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "neighborhood", length = 128)
    private String neighborhood;

    @Column(name = "ward_number")
    private Integer wardNumber;

    @Column(name = "ward_name", length = 128)
    private String wardName;

    @Column(name = "municipal_body", length = 128)
    private String municipalBody = "Delhi Jal Board / MCD";

    @Column(name = "citizen_name", length = 128)
    private String citizenName;

    @Column(name = "citizen_phone", length = 32)
    private String citizenPhone;

    @Column(name = "citizen_email", length = 128)
    private String citizenEmail;

    @Column(name = "device_model", length = 128)
    private String deviceModel;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReportStatus status = ReportStatus.SUBMITTED;

    @Column(name = "status_notes", length = 1000)
    private String statusNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "citizen_rating")
    private Integer citizenRating;

    @Column(name = "citizen_feedback_comment", length = 500)
    private String citizenFeedbackComment;

    @Column(name = "feedback_submitted_at")
    private LocalDateTime feedbackSubmittedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.reportedAt == null) {
            this.reportedAt = LocalDateTime.now();
        }
        if (this.capturedAt == null) {
            this.capturedAt = this.reportedAt;
        }
        if (this.latitude != null && this.longitude != null && this.location == null) {
            syncLocationFromCoordinates();
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        if (this.latitude != null && this.longitude != null) {
            syncLocationFromCoordinates();
        }
    }

    public void syncLocationFromCoordinates() {
        if (this.latitude != null && this.longitude != null) {
            this.location = GEOMETRY_FACTORY.createPoint(new Coordinate(this.longitude, this.latitude));
            this.location.setSRID(4326);
        }
    }
}
