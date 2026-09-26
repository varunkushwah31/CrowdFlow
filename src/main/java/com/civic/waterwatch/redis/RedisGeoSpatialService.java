package com.civic.waterwatch.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * In-Memory Geospatial Index powered by Redis Native Geo commands (GEOADD, GEORADIUS).
 * Enables sub-millisecond proximity queries across Delhi coordinates before querying PostGIS.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisGeoSpatialService {

    private final StringRedisTemplate stringRedisTemplate;

    public static final String GEO_REPORTS_KEY = "crowdflow:geo:reports";

    /**
     * Indexes a water report in the Redis geospatial set using WGS-84 coordinates.
     * Note: Redis Geo uses (longitude, latitude) order.
     *
     * @param reportCode Unique grievance tracking code (e.g. IND-H2O-1686)
     * @param latitude   GPS Latitude
     * @param longitude  GPS Longitude
     */
    public void indexReportLocation(String reportCode, Double latitude, Double longitude) {
        if (reportCode == null || latitude == null || longitude == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForGeo().add(
                    GEO_REPORTS_KEY,
                    new Point(longitude, latitude),
                    reportCode
            );
            log.debug("Indexed report {} in Redis Geo ({}, {})", reportCode, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to index report {} in Redis Geo: {}", reportCode, e.getMessage());
        }
    }

    /**
     * Finds nearby report codes within the specified radius in kilometers.
     *
     * @param latitude  Center latitude
     * @param longitude Center longitude
     * @param radiusKm  Search radius in kilometers (e.g. 0.5 for 500m)
     * @return List of nearby report codes
     */
    public List<String> findNearbyReportCodes(Double latitude, Double longitude, double radiusKm) {
        List<String> results = new ArrayList<>();
        if (latitude == null || longitude == null) {
            return results;
        }

        try {
            Circle circle = new Circle(
                    new Point(longitude, latitude),
                    new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS)
            );

            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                    stringRedisTemplate.opsForGeo().radius(GEO_REPORTS_KEY, circle);

            if (geoResults != null) {
                geoResults.forEach(res -> results.add(res.getContent().getName()));
            }
        } catch (Exception e) {
            log.error("Failed to query Redis Geo nearby reports: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Removes a report from the geospatial index.
     */
    public void removeReportLocation(String reportCode) {
        try {
            stringRedisTemplate.opsForZSet().remove(GEO_REPORTS_KEY, reportCode);
        } catch (Exception e) {
            log.error("Failed to remove report {} from Redis Geo: {}", reportCode, e.getMessage());
        }
    }

    /**
     * Returns the total count of reports indexed in Redis Geospatial memory.
     */
    public Long getIndexedReportCount() {
        try {
            return stringRedisTemplate.opsForZSet().size(GEO_REPORTS_KEY);
        } catch (Exception e) {
            return 0L;
        }
    }
}
