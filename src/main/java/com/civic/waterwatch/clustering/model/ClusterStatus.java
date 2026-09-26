package com.civic.waterwatch.clustering.model;

import lombok.Getter;

@Getter
public enum ClusterStatus {
    ACTIVE("Active - Reports Accumulating"),
    ESCALATED("Escalated - Forwarded to Jal Board / Ward Control Room"),
    UNDER_INVESTIGATION("Under Investigation - JE / AEE Field Unit Deployed"),
    IN_PROGRESS("In Progress - Repair & Restoration Underway"),
    RESOLVED("Resolved - Supply & Pipeline Restored"),
    CLOSED("Closed - Verified by Residents");

    private final String description;

    ClusterStatus(String description) {
        this.description = description;
    }
}
