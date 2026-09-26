package com.civic.waterwatch.clustering.event;

import com.civic.waterwatch.clustering.model.IncidentCluster;
import org.springframework.context.ApplicationEvent;

public class ClusterThresholdReachedEvent extends ApplicationEvent {

    private final IncidentCluster cluster;
    private final String triggerReason;

    public ClusterThresholdReachedEvent(Object source, IncidentCluster cluster, String triggerReason) {
        super(source);
        this.cluster = cluster;
        this.triggerReason = triggerReason;
    }

    public IncidentCluster getCluster() {
        return cluster;
    }

    public String getTriggerReason() {
        return triggerReason;
    }
}
