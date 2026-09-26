package com.civic.waterwatch.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingMilestone {
    private String stepKey;       // SUBMITTED, TRIAGED, CLUSTERED, DISPATCHED, RESOLVED
    private String title;         // Display title (e.g. "Report Received & GPS Verified")
    private String description;   // Detailed milestone text
    private String icon;          // FontAwesome icon class
    private String status;        // COMPLETED, CURRENT, PENDING
    private LocalDateTime timestamp;
    private String actor;         // "Citizen via Mobile Client", "Delhi Jal Board Ward 85", "Repair Unit"
}
