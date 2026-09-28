package com.civic.waterwatch.geo.controller;

import com.civic.waterwatch.exception.InvalidCoordinateException;
import com.civic.waterwatch.geo.model.CoordinateValidationResult;
import com.civic.waterwatch.geo.model.DispatchRoutePlan;
import com.civic.waterwatch.geo.model.EmergencyDepot;
import com.civic.waterwatch.geo.model.WaterTankerUnit;
import com.civic.waterwatch.geo.service.GpsCoordinateService;
import com.civic.waterwatch.geo.service.MunicipalCoordinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for GPS, Geodesic Coordinate Conversions,
 * Delhi Emergency Fleet Coordination, and Tactical Incident Dispatching.
 */
@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
@Tag(name = "GPS & Fleet Coordination (India Civic)", description = "Endpoints for GPS validation, UTM/DMS/Grid conversions, DJB emergency depots, tanker tracking, and route dispatch")
public class GpsCoordinationController {

    private final GpsCoordinateService gpsCoordinateService;
    private final MunicipalCoordinationService municipalCoordinationService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoordinateRequest {
        private Double latitude;
        private Double longitude;
        private String description;
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate GPS coordinates and convert to DMS, UTM Zone 43N, Indian Grid, Plus Codes and resolve Ward")
    public ResponseEntity<CoordinateValidationResult> validateGet(
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon
    ) {
        return ResponseEntity.ok(gpsCoordinateService.validateAndEnrich(lat, lon));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate GPS coordinates submitted via JSON body")
    public ResponseEntity<CoordinateValidationResult> validatePost(
            @RequestBody CoordinateRequest request
    ) {
        Double lat = request != null ? request.getLatitude() : null;
        Double lon = request != null ? request.getLongitude() : null;
        return ResponseEntity.ok(gpsCoordinateService.validateAndEnrich(lat, lon));
    }

    @GetMapping("/depots")
    @Operation(summary = "List all 6 Delhi Jal Board & MCD emergency rapid response depots and water bases")
    public ResponseEntity<List<EmergencyDepot>> getAllDepots() {
        return ResponseEntity.ok(municipalCoordinationService.getAllDepots());
    }

    @GetMapping("/tankers")
    @Operation(summary = "Get live GPS telemetry and operational status for active Delhi potable water tanker fleet")
    public ResponseEntity<List<WaterTankerUnit>> getActiveTankers() {
        return ResponseEntity.ok(municipalCoordinationService.getActiveTankers());
    }

    @GetMapping("/coordinate-dispatch")
    @Operation(summary = "Calculate nearest depot, traffic-adjusted ETA, vehicle type, and navigation waypoints to coordinates")
    public ResponseEntity<DispatchRoutePlan> coordinateDispatch(
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon,
            @RequestParam(value = "desc", required = false, defaultValue = "CIVIC_WATER_EMERGENCY") String desc
    ) {
        if (lat == null || lon == null) {
            throw new InvalidCoordinateException(lat, lon, "Both latitude and longitude parameters are required for emergency dispatch");
        }
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            throw new InvalidCoordinateException(lat, lon, "Latitude must be in [-90, 90] and longitude in [-180, 180]");
        }
        return ResponseEntity.ok(municipalCoordinationService.coordinateDispatch(lat, lon, desc));
    }

    @GetMapping("/coordinate-dispatch/cluster/{clusterId}")
    @Operation(summary = "Calculate emergency dispatch plan and route geometry for an existing DBSCAN incident cluster")
    public ResponseEntity<DispatchRoutePlan> coordinateDispatchForCluster(
            @PathVariable("clusterId") Long clusterId
    ) {
        return ResponseEntity.ok(municipalCoordinationService.coordinateDispatchForCluster(clusterId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search landmark geocoding catalog for Delhi NCR civic spots, metro hubs, and municipal wards")
    public ResponseEntity<List<Map<String, Object>>> searchLandmarks(
            @RequestParam("q") String query
    ) {
        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException("Search query must be at least 2 characters long. Provided: '" + query + "'");
        }
        return ResponseEntity.ok(municipalCoordinationService.searchLandmarks(query.trim()));
    }

    @GetMapping("/distance")
    @Operation(summary = "High-precision geodesic distance (Haversine), road distance, bearing, and compass heading between two GPS points")
    public ResponseEntity<Map<String, Object>> calculateDistance(
            @RequestParam("lat1") Double lat1,
            @RequestParam("lon1") Double lon1,
            @RequestParam("lat2") Double lat2,
            @RequestParam("lon2") Double lon2
    ) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new InvalidCoordinateException("All 4 coordinate parameters (lat1, lon1, lat2, lon2) must be provided");
        }
        if (lat1 < -90.0 || lat1 > 90.0 || lat2 < -90.0 || lat2 > 90.0 ||
            lon1 < -180.0 || lon1 > 180.0 || lon2 < -180.0 || lon2 > 180.0) {
            throw new InvalidCoordinateException("Coordinates out of terrestrial bounds [-90, 90] for latitude and [-180, 180] for longitude");
        }

        double distKm = gpsCoordinateService.calculateDistanceKm(lat1, lon1, lat2, lon2);
        double bearing = gpsCoordinateService.calculateBearing(lat1, lon1, lat2, lon2);
        String compass = gpsCoordinateService.getCompassDirection(bearing);
        double roadDistKm = Math.round((distKm * 1.28) * 100.0) / 100.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("geodesicDistanceKm", Math.round(distKm * 1000.0) / 1000.0);
        result.put("geodesicDistanceMeters", Math.round(distKm * 1000.0));
        result.put("estimatedRoadDistanceKm", roadDistKm);
        result.put("bearingDegrees", Math.round(bearing * 10.0) / 10.0);
        result.put("compassHeading", compass);

        return ResponseEntity.ok(result);
    }
}
