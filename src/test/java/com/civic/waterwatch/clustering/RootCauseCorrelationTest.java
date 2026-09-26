package com.civic.waterwatch.clustering;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.RootCauseAnalysis;
import com.civic.waterwatch.clustering.service.RootCauseCorrelationEngine;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.WaterReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RootCauseCorrelationTest {

    private RootCauseCorrelationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RootCauseCorrelationEngine();
    }

    private WaterReport createMockReport(IssueType issueType) {
        WaterReport r = new WaterReport();
        r.setIssueType(issueType);
        r.setLatitude(28.6139);
        r.setLongitude(77.2090);
        return r;
    }

    @Test
    @DisplayName("Should deduce Major Main Line Rupture when Burst Pipe and Waterlogging/Low Pressure co-occur")
    void testMajorMainFractureDeduction() {
        List<WaterReport> reports = List.of(
                createMockReport(IssueType.BURST_PIPE),
                createMockReport(IssueType.SEVERE_WATERLOGGING),
                createMockReport(IssueType.LOW_PRESSURE),
                createMockReport(IssueType.SEVERE_WATERLOGGING)
        );

        RootCauseAnalysis analysis = engine.analyze(reports);

        assertEquals("MAIN_LINE_BREAK", analysis.getFailureCategory());
        assertEquals(ClusterSeverity.CRITICAL, analysis.getSuggestedSeverity());
        assertTrue(analysis.getPrimaryHypothesis().contains("Pipeline") || analysis.getPrimaryHypothesis().contains("Rupture"));
        assertTrue(analysis.getConfidenceScore() >= 80.0);
    }

    @Test
    @DisplayName("Should deduce Sewage Cross-Contamination when Contamination and Open Sewage co-occur")
    void testCrossContaminationDeduction() {
        List<WaterReport> reports = List.of(
                createMockReport(IssueType.CONTAMINATION),
                createMockReport(IssueType.OPEN_SEWAGE),
                createMockReport(IssueType.DRAINAGE_OVERFLOW),
                createMockReport(IssueType.CONTAMINATION)
        );

        RootCauseAnalysis analysis = engine.analyze(reports);

        assertEquals("CROSS_CONTAMINATION_EVENT", analysis.getFailureCategory());
        assertEquals(ClusterSeverity.CRITICAL, analysis.getSuggestedSeverity());
        assertTrue(analysis.getRecommendedAction().contains("Boil Water"));
    }

    @Test
    @DisplayName("Should deduce Pressure Grid Deficit when Scarcity and Low Pressure co-occur without surface leaks")
    void testPressureGridDeficitDeduction() {
        List<WaterReport> reports = List.of(
                createMockReport(IssueType.LOW_PRESSURE),
                createMockReport(IssueType.WATER_SCARCITY),
                createMockReport(IssueType.WATER_SCARCITY)
        );

        RootCauseAnalysis analysis = engine.analyze(reports);

        assertEquals("PRESSURE_GRID_DEFICIT", analysis.getFailureCategory());
        assertEquals(ClusterSeverity.HIGH, analysis.getSuggestedSeverity());
    }

    @Test
    @DisplayName("Should deduce Distribution Joint Leakage when multiple chronic leaks are clustered")
    void testJointLeakageDeduction() {
        List<WaterReport> reports = List.of(
                createMockReport(IssueType.LEAKAGE),
                createMockReport(IssueType.LEAKAGE),
                createMockReport(IssueType.LEAKAGE)
        );

        RootCauseAnalysis analysis = engine.analyze(reports);

        assertEquals("DISTRIBUTION_JOINT_LEAKAGE", analysis.getFailureCategory());
        assertEquals(ClusterSeverity.MEDIUM, analysis.getSuggestedSeverity());
    }
}
