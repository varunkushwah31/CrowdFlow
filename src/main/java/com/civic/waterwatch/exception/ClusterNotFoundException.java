package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an incident cluster ID or cluster code is not found in the repository.
 */
public class ClusterNotFoundException extends WaterWatchException {

    public ClusterNotFoundException(Long clusterId) {
        super(
                "Incident cluster not found with ID: " + clusterId,
                HttpStatus.NOT_FOUND,
                "CLUSTER_NOT_FOUND",
                Map.of("clusterId", clusterId)
        );
    }

    public ClusterNotFoundException(String clusterCode) {
        super(
                "Incident cluster not found with code: " + clusterCode,
                HttpStatus.NOT_FOUND,
                "CLUSTER_NOT_FOUND",
                Map.of("clusterCode", clusterCode)
        );
    }
}
