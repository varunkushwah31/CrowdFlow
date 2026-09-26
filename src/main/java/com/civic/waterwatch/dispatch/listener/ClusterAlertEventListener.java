package com.civic.waterwatch.dispatch.listener;

import com.civic.waterwatch.clustering.event.ClusterThresholdReachedEvent;
import com.civic.waterwatch.dispatch.service.MunicipalDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterAlertEventListener {

    private final MunicipalDispatchService dispatchService;

    @Async
    @EventListener
    public void handleClusterAlert(ClusterThresholdReachedEvent event) {
        log.info("Async Event Received: ClusterThresholdReachedEvent for Indian Civic Cluster {} (Trigger: {})",
                event.getCluster().getClusterCode(), event.getTriggerReason());

        try {
            dispatchService.dispatchEscalation(event.getCluster());
            log.info("Municipal dispatch protocol executed successfully for Cluster {}", event.getCluster().getClusterCode());
        } catch (Exception e) {
            log.error("Failed during automated municipal dispatch for Cluster {}: {}",
                    event.getCluster().getClusterCode(), e.getMessage(), e);
        }
    }
}
