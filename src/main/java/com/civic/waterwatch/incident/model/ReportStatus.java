package com.civic.waterwatch.incident.model;

import lombok.Getter;

@Getter
public enum ReportStatus {
    SUBMITTED("Submitted - Awaiting Geospatial Clustering"),
    CLUSTERED("Clustered - Grouped into Ward Incident Zone"),
    ESCALATED("Escalated - Forwarded to Jal Board / Ward Control Room"),
    IN_PROGRESS("In Progress - Field Maintenance Crew Dispatched"),
    RESOLVED("Resolved - Pipeline Restored & Verified"),
    REJECTED("Rejected - False Alarm or Duplicate Grievance");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }
}
