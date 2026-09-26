package com.civic.waterwatch.clustering.service;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.RootCauseAnalysis;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.WaterReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RootCauseCorrelationEngine {

    public RootCauseAnalysis analyze(List<WaterReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return RootCauseAnalysis.builder()
                    .primaryHypothesis("Insufficient Citizen Data")
                    .confidenceScore(0.0)
                    .failureCategory("UNKNOWN")
                    .suggestedSeverity(ClusterSeverity.LOW)
                    .technicalSummary("No citizen reports available for geospatial correlation.")
                    .recommendedAction("Awaiting further crowdsourced reports from ward residents.")
                    .symptomDistribution(Map.of())
                    .build();
        }

        // 1. Calculate symptom distribution
        Map<IssueType, Long> counts = new EnumMap<>(IssueType.class);
        for (IssueType type : IssueType.values()) {
            counts.put(type, 0L);
        }
        for (WaterReport report : reports) {
            counts.put(report.getIssueType(), counts.getOrDefault(report.getIssueType(), 0L) + 1);
        }

        long burstPipeCount = counts.getOrDefault(IssueType.BURST_PIPE, 0L);
        long leakageCount = counts.getOrDefault(IssueType.LEAKAGE, 0L);
        long contaminationCount = counts.getOrDefault(IssueType.CONTAMINATION, 0L);
        long waterloggingCount = counts.getOrDefault(IssueType.SEVERE_WATERLOGGING, 0L);
        long drainageCount = counts.getOrDefault(IssueType.DRAINAGE_OVERFLOW, 0L);
        long openSewageCount = counts.getOrDefault(IssueType.OPEN_SEWAGE, 0L);
        long lowPressureCount = counts.getOrDefault(IssueType.LOW_PRESSURE, 0L);
        long scarcityCount = counts.getOrDefault(IssueType.WATER_SCARCITY, 0L);
        long borewellCount = counts.getOrDefault(IssueType.BOREWELL_DEPLETION, 0L);

        int totalReports = reports.size();

        // 2. Pattern A: Major Transmission Feeder Pipeline Burst (Jal Board Arterial Rupture)
        if (burstPipeCount > 0 && (waterloggingCount > 0 || lowPressureCount > 0 || totalReports >= 4)) {
            double confidence = Math.min(98.5, 76.0 + (burstPipeCount * 5.0) + (waterloggingCount * 4.0) + (lowPressureCount * 3.5));
            return RootCauseAnalysis.builder()
                    .primaryHypothesis("Major Transmission Feeder Pipeline Burst (Jal Board Arterial Rupture)")
                    .confidenceScore(confidence)
                    .failureCategory("MAIN_LINE_BREAK")
                    .suggestedSeverity(ClusterSeverity.CRITICAL)
                    .technicalSummary(String.format("Correlated %d citizen grievances indicating a high-pressure transmission line breach from WTP/UGR co-occurring with road waterlogging and domestic pressure collapse.", totalReports))
                    .recommendedAction("IMMEDIATE JAL BOARD EMERGENCY DISPATCH: Mobilize Quick Response Pipeline Van. Isolate Sluice Valve SV-04B at Pumping Booster Station; deploy municipal dewatering pumps to prevent road collapse; activate emergency water tanker protocol for ward.")
                    .symptomDistribution(counts)
                    .build();
        }

        // 3. Pattern B: Sewage Cross-Contamination & Biohazard Hazard (AMRUT / Swachh Bharat Protocol)
        if (contaminationCount > 0 && (openSewageCount > 0 || drainageCount > 0)) {
            double confidence = Math.min(99.0, 82.0 + (contaminationCount * 6.0) + (openSewageCount * 6.0));
            return RootCauseAnalysis.builder()
                    .primaryHypothesis("Sewage Infiltration & Cross-Contamination into Potable Water Network")
                    .confidenceScore(confidence)
                    .failureCategory("CROSS_CONTAMINATION_EVENT")
                    .suggestedSeverity(ClusterSeverity.CRITICAL)
                    .technicalSummary(String.format("Multi-source geospatial correlation confirms domestic tap water foul odor and turbidity occurring adjacent to overflowing nallah/manhole across %d reporting households. Severe fecal coliform and pathogen hazard detected.", totalReports))
                    .recommendedAction("CRITICAL CIVIC HEALTH DIRECTIVE: Issue Immediate Ward-wide 'Boil Water Advisory' under National Jal Jeevan / AMRUT guidelines. Deploy Jal Board Mobile Chlorination & Water Quality Van. Deploy Super-Sucker vacuum truck to unchoke clogged nallah sump.")
                    .symptomDistribution(counts)
                    .build();
        }

        // 4. Pattern C: Zonal Pumping Grid Failure / Feeder Starvation
        if ((lowPressureCount > 0 && scarcityCount > 0) || (scarcityCount >= 3 && burstPipeCount == 0 && waterloggingCount == 0) || borewellCount > 1) {
            double confidence = Math.min(94.0, 72.0 + (lowPressureCount * 4.0) + (scarcityCount * 5.0));
            return RootCauseAnalysis.builder()
                    .primaryHypothesis("Zonal Booster Pumping Station Trip or Feeder Valve Closure")
                    .confidenceScore(confidence)
                    .failureCategory("PRESSURE_GRID_DEFICIT")
                    .suggestedSeverity(ClusterSeverity.HIGH)
                    .technicalSummary(String.format("Widespread reports of dry taps and pressure depletion across %d residential households without localized surface leaks points to upstream pumping station power trip or closed district gateway valve.", totalReports))
                    .recommendedAction("ACTION: Check Jal Board sector booster pumping station telemetry and reservoir head levels. Contact Central Water Emergency Helpline (1916) for backup feeder supply.")
                    .symptomDistribution(counts)
                    .build();
        }

        // 5. Pattern D: Monsoon Storm Nallah Siltation & Underpass Inundation
        if (waterloggingCount > 0 && drainageCount > 0) {
            double confidence = Math.min(93.0, 74.0 + (waterloggingCount * 5.0) + (drainageCount * 5.0));
            return RootCauseAnalysis.builder()
                    .primaryHypothesis("Arterial Storm Nallah Siltation & Culvert Sump Inundation")
                    .confidenceScore(confidence)
                    .failureCategory("STORM_DRAINAGE_BACKUP")
                    .suggestedSeverity(ClusterSeverity.HIGH)
                    .technicalSummary(String.format("Localized street inundation reported across %d locations due to storm drain siltation and plastic waste choking underground culvert sumps.", totalReports))
                    .recommendedAction("ACTION: Deploy MCD / Municipal Corporation Hydro-Vac suction jetting trucks; clear arterial storm culverts and open emergency runoff bypass gates.")
                    .symptomDistribution(counts)
                    .build();
        }

        // 6. Pattern E: Chronic Pipe Joint Fatigue & Seepage
        if (leakageCount >= 2 || totalReports >= 3) {
            double confidence = Math.min(91.0, 68.0 + (leakageCount * 6.0));
            return RootCauseAnalysis.builder()
                    .primaryHypothesis("Aging Cast Iron / Ductile Iron Pipeline Joint Seepage")
                    .confidenceScore(confidence)
                    .failureCategory("DISTRIBUTION_JOINT_LEAKAGE")
                    .suggestedSeverity(ClusterSeverity.MEDIUM)
                    .technicalSummary(String.format("Persistent seepage clustered across %d locations along the same road segment indicates joint slippage or corrosion on secondary distribution line.", totalReports))
                    .recommendedAction("ACTION: Schedule acoustic ground microphone survey; dispatch Jal Board maintenance van for pipe sleeve clamp installation within 24-48 hours.")
                    .symptomDistribution(counts)
                    .build();
        }

        // Default: Emerging Localized Hazard
        IssueType dominantType = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(IssueType.OTHER);

        ClusterSeverity severity = (dominantType == IssueType.CONTAMINATION || dominantType == IssueType.OPEN_SEWAGE)
                ? ClusterSeverity.HIGH : ClusterSeverity.LOW;

        return RootCauseAnalysis.builder()
                .primaryHypothesis("Clustered Water Hazard: " + dominantType.getDisplayName())
                .confidenceScore(62.0 + (totalReports * 3.0))
                .failureCategory("EMERGING_CLUSTER")
                .suggestedSeverity(severity)
                .technicalSummary(String.format("Geospatial cluster of %d complaints focused predominantly on %s.", totalReports, dominantType.getDisplayName()))
                .recommendedAction("ACTION: Monitor cluster growth and assign Ward Junior Engineer (JE - Water) for site verification.")
                .symptomDistribution(counts)
                .build();
    }
}
