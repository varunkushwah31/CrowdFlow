package com.civic.waterwatch.clustering.model;

import com.civic.waterwatch.incident.model.IssueType;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RootCauseAnalysis {
    private String primaryHypothesis;
    private double confidenceScore;
    private String failureCategory;
    private ClusterSeverity suggestedSeverity;
    private String technicalSummary;
    private String recommendedAction;
    private Map<IssueType, Long> symptomDistribution;
}
