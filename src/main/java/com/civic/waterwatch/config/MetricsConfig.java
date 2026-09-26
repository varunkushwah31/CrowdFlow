package com.civic.waterwatch.config;

import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;

/**
 * Exposes production Prometheus metrics for municipal dashboards & Grafana.
 * - Active unclustered complaints
 * - Resolved citizen grievances
 * - Active emergency infrastructure clusters
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final WaterReportRepository reportRepository;
    private final IncidentClusterRepository clusterRepository;

    @PostConstruct
    public void registerCustomMetrics() {
        Gauge.builder("crowdflow_reports_submitted_total", reportRepository,
                repo -> repo.countByStatus(ReportStatus.SUBMITTED))
                .description("Total citizen water grievance reports pending triage")
                .register(meterRegistry);

        Gauge.builder("crowdflow_reports_clustered_total", reportRepository,
                WaterReportRepository::countByClusterIdIsNotNull)
                .description("Total citizen reports correlated into spatial clusters")
                .register(meterRegistry);

        Gauge.builder("crowdflow_reports_resolved_total", reportRepository,
                repo -> repo.countByStatus(ReportStatus.RESOLVED))
                .description("Total citizen water complaints resolved by municipal engineers")
                .register(meterRegistry);

        Gauge.builder("crowdflow_clusters_active_total", clusterRepository,
                        CrudRepository::count)
                .description("Total detected water infrastructure emergency clusters")
                .register(meterRegistry);
    }
}
