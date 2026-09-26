package com.civic.waterwatch.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveGrievanceTrackingDto implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String reportCode;
    private String issueType;
    private String issueTypeName;
    private String category;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String address;
    private String neighborhood;
    private Integer wardNumber;
    private String wardName;
    private String municipalBody;
    private LocalDateTime reportedAt;

    // Live Progress
    private String currentStage; // SUBMITTED, WARD_TRIAGED, CLUSTERED, DISPATCHED, IN_PROGRESS, RESOLVED, CLOSED
    private int progressPercentage; // 20%, 40%, 60%, 80%, 100%
    private String statusDescription;
    private String latestFieldNotes;
    private LocalDateTime lastUpdatedAt;

    // Nodal Officer & Dispatch Contact
    private String officerName;
    private String officerRole;
    private String officerContact;
    private String officerEmail;
    private String helpline; // 1916 (Delhi Jal Board)
    private String estimatedResolutionTime; // e.g. "Within 4 Hours (Urgent Arterial Rupture)"

    // Correlated Cluster Context
    private Long clusterId;
    private String clusterCode;
    private String clusterSeverity;
    private String rootCauseSummary;
    private String recommendedRemedy;
    private Integer neighborReportCount; // e.g. 5 citizen reports in 150m radius
    private String pdfDossierUrl;

    // Live Step-by-Step Milestones
    private List<TrackingMilestone> milestones;

    // Citizen Feedback Loop
    private Integer citizenRating; // 1 to 5
    private String citizenFeedbackComment;
    private LocalDateTime feedbackSubmittedAt;
}
