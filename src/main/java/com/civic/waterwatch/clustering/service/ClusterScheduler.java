package com.civic.waterwatch.clustering.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "waterwatch.clustering.cron-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ClusterScheduler {

    private final SpatialClusteringService clusteringService;

    @Scheduled(cron = "${waterwatch.clustering.cron-expression:0 */2 * * * *}")
    public void scheduledClusterExecution() {
        log.debug("Triggering scheduled spatial DBSCAN clustering cycle across Indian ward boundaries...");
        try {
            SpatialClusteringService.ClusteringRunSummary summary = clusteringService.runClustering();
            if (summary.getClustersFound() > 0) {
                log.info("Scheduled clustering completed: {} active Indian civic clusters, {} citizen grievances grouped.",
                        summary.getClustersFound(), summary.getReportsClustered());
            }
        } catch (Exception e) {
            log.error("Scheduled clustering encountered an error: {}", e.getMessage(), e);
        }
    }
}
