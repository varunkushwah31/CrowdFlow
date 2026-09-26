package com.civic.waterwatch.incident.model;

import lombok.Getter;

@Getter
public enum IssueType {
    BURST_PIPE("Main Pipeline Burst / Jal Board Arterial Rupture", "DISTRIBUTION", 4),
    LEAKAGE("Visible Pipeline Seepage / Road Curb Leak", "DISTRIBUTION", 2),
    CONTAMINATION("Contaminated / Turbid Tap Water (Sewage Mixing)", "QUALITY", 5),
    SEVERE_WATERLOGGING("Severe Waterlogging / Underpass Inundation", "DRAINAGE", 4),
    DRAINAGE_OVERFLOW("Open Nallah / Choked Stormwater Drain Overflow", "DRAINAGE", 3),
    OPEN_SEWAGE("Open Sewage & Overflowing Manhole Hazard", "SEWAGE", 5),
    LOW_PRESSURE("Extremely Low Pressure / Booster Pump Failure", "SUPPLY", 2),
    WATER_SCARCITY("No Municipal Supply / Delayed Water Tanker", "SUPPLY", 3),
    BOREWELL_DEPLETION("Borewell Dried Up / Groundwater Depletion", "SUPPLY", 3),
    OTHER("Other Civic Water Infrastructure Hazard", "GENERAL", 1);

    private final String displayName;
    private final String category;
    private final int severityWeight;

    IssueType(String displayName, String category, int severityWeight) {
        this.displayName = displayName;
        this.category = category;
        this.severityWeight = severityWeight;
    }
}
