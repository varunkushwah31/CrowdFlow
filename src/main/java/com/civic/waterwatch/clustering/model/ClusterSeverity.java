package com.civic.waterwatch.clustering.model;

import lombok.Getter;

@Getter
public enum ClusterSeverity {
    CRITICAL("CRITICAL - Immediate Emergency Action Required (Jal Board / NDMC)", 4, "#dc2626"),
    HIGH("HIGH - Priority Escalation within 12 Hours", 3, "#ea580c"),
    MEDIUM("MEDIUM - Standard Maintenance Dispatch (24-48h)", 2, "#d97706"),
    LOW("LOW - Localized Monitored Anomaly", 1, "#2563eb");

    private final String label;
    private final int level;
    private final String hexColor;

    ClusterSeverity(String label, int level, String hexColor) {
        this.label = label;
        this.level = level;
        this.hexColor = hexColor;
    }
}
