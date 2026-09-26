package com.civic.waterwatch.clustering;

import com.civic.waterwatch.clustering.service.SpatialClusteringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpatialClusteringDistanceTest {

    @Test
    @DisplayName("Should accurately calculate geodesic distance between coordinates in meters")
    void testHaversineDistance() {
        // Points ~100m apart in central metro grid
        double lat1 = 28.6320;
        double lon1 = 77.2105;
        double lat2 = 28.6328;
        double lon2 = 77.2108;

        double distance = SpatialClusteringService.haversineDistanceMeters(lat1, lon1, lat2, lon2);

        // Distance should be approximately 93 meters
        assertTrue(distance > 80.0 && distance < 110.0, "Calculated distance: " + distance);
    }

    @Test
    @DisplayName("Distance to identical coordinates must be zero")
    void testZeroDistance() {
        double dist = SpatialClusteringService.haversineDistanceMeters(28.6139, 77.2090, 28.6139, 77.2090);
        assertEquals(0.0, dist, 0.0001);
    }
}
