package com.civic.waterwatch.geo.service;

import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.geo.model.DispatchRoutePlan;
import com.civic.waterwatch.geo.model.EmergencyDepot;
import com.civic.waterwatch.geo.model.WaterTankerUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

/**
 * Service orchestrating emergency fleet coordination, nearest depot allocation,
 * and dispatch navigation routing for Delhi Jal Board (DJB) and MCD operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MunicipalCoordinationService {

    private final GpsCoordinateService gpsCoordinateService;
    private final IncidentClusterRepository clusterRepository;

    // 6 Master Emergency Depots & Jal Board Operational Bases
    private final List<EmergencyDepot> emergencyDepots = List.of(
            new EmergencyDepot("DEPOT-DJB-01", "DJB Central Operations Control Room (Varunalaya HQ)",
                    "VARUNALAYA-HQ", "CENTRAL_HEADQUARTERS", 28.6475, 77.1990,
                    "Varunalaya Phase II, Jhandewalan, Karol Bagh, New Delhi - 110005",
                    "Er. Rakesh K. Kaushik (Chief Engineer)", "+91 11 2351 5000", "VHF-CH-01", 18, 12, "OPERATIONAL"),

            new EmergencyDepot("DEPOT-DJB-85", "Ward 85 Rapid Emergency Depot (Pusa Road Booster)",
                    "PUSA-BOOSTER-85", "RAPID_RESPONSE_DEPOT", 28.6440, 77.1930,
                    "Near Metro Pillar 118, Pusa Road, Karol Bagh, New Delhi - 110005",
                    "Shri Alok Sharma (EE - Central)", "+91 98110 23412", "VHF-CH-04", 8, 6, "OPERATIONAL"),

            new EmergencyDepot("DEPOT-DJB-142", "Ward 142 South Emergency Depot (Lajpat Nagar)",
                    "LAJPAT-SOUTH-142", "RAPID_RESPONSE_DEPOT", 28.5710, 77.2415,
                    "Ring Road Utility Yard, Lajpat Nagar Part IV, New Delhi - 110024",
                    "Smt. Sunita Rao (AEE - South)", "+91 98711 54321", "VHF-CH-08", 10, 7, "OPERATIONAL"),

            new EmergencyDepot("DEPOT-DJB-65", "Ward 65 North-West Maintenance Base (Rohini Sector 14)",
                    "ROHINI-NW-65", "RAPID_RESPONSE_DEPOT", 28.7160, 77.1290,
                    "Institutional Area, Outer Ring Road, Rohini Sector 14, Delhi - 110085",
                    "Er. Rajesh Verma (EE - North West)", "+91 99100 87654", "VHF-CH-12", 9, 8, "OPERATIONAL"),

            new EmergencyDepot("DEPOT-DJB-210", "Ward 210 Trans-Yamuna Drainage Base (Mayur Vihar 1)",
                    "MAYUR-VIHAR-210", "DRAINAGE_DEPOT", 28.6090, 77.2970,
                    "Pocket 1 Nallah Bypass Road, Mayur Vihar Phase 1, Delhi - 110091",
                    "Er. Mohammad Farhan (AEE - Drainage)", "+91 98188 34567", "VHF-CH-16", 6, 10, "OPERATIONAL"),

            new EmergencyDepot("DEPOT-NDMC-104", "Ward 104 NDMC Water Grievance Cell (Barakhamba)",
                    "NDMC-CP-104", "CIVIC_HOTSPOT_CELL", 28.6300, 77.2210,
                    "Palika Kendra Substation, Barakhamba Road, Connaught Place, New Delhi - 110001",
                    "Er. Vikramaditya Sen (Superintending Engineer)", "+91 11 2336 5432", "VHF-CH-20", 5, 5, "OPERATIONAL")
    );

    // Active Simulated Jal Board Water Tanker Fleet
    private final List<WaterTankerUnit> tankerFleet = List.of(
            new WaterTankerUnit("DL-1M-4512", 9000, "PUSA-BOOSTER-85", "Suresh Chand", "+91 98114 56789", "DISPATCHED", 28.6448, 77.1948, 75.0, "Ward 85 - Karol Bagh"),
            new WaterTankerUnit("DL-1M-7833", 6000, "LAJPAT-SOUTH-142", "Ram Avtar Yadav", "+91 98713 45678", "IDLE_AT_DEPOT", 28.5710, 77.2415, 0.0, "Ward 142 - Lajpat Nagar"),
            new WaterTankerUnit("DL-1M-9021", 9000, "MAYUR-VIHAR-210", "Jagdish Prasad", "+91 99115 67890", "DISPATCHED", 28.6083, 77.2975, 180.0, "Ward 210 - Mayur Vihar"),
            new WaterTankerUnit("DL-1M-3344", 6000, "ROHINI-NW-65", "Kuldeep Singh", "+91 98108 12345", "IDLE_AT_DEPOT", 28.7160, 77.1290, 0.0, "Ward 65 - Rohini"),
            new WaterTankerUnit("DL-1M-6611", 12000, "NDMC-CP-104", "Mohan Lal", "+91 11 2334 9876", "FILLING_AT_HYDRANT", 28.6305, 77.2215, 45.0, "Ward 104 - Connaught Place")
    );

    public List<EmergencyDepot> getAllDepots() {
        return emergencyDepots;
    }

    public List<WaterTankerUnit> getActiveTankers() {
        return tankerFleet;
    }

    /**
     * Determines the optimal emergency depot, calculates geodesic & road distances,
     * factors in Delhi traffic conditions, and generates full dispatch navigation waypoints.
     */
    public DispatchRoutePlan coordinateDispatch(Double targetLat, Double targetLon, String targetDescription) {
        if (targetLat == null || targetLon == null) {
            targetLat = 28.6445;
            targetLon = 77.1950;
        }

        // 1. Find Nearest Depot
        EmergencyDepot nearestDepot = null;
        double minDistanceKm = Double.MAX_VALUE;

        for (EmergencyDepot depot : emergencyDepots) {
            double dist = gpsCoordinateService.calculateDistanceKm(
                    depot.getLatitude(), depot.getLongitude(), targetLat, targetLon
            );
            if (dist < minDistanceKm) {
                minDistanceKm = dist;
                nearestDepot = depot;
            }
        }

        if (nearestDepot == null) {
            nearestDepot = emergencyDepots.getFirst();
            minDistanceKm = 1.0;
        }

        double geodesicKm = Math.round(minDistanceKm * 100.0) / 100.0;
        // Delhi arterial road winding factor (approx 1.28x Euclidean)
        double roadKm = Math.round((geodesicKm * 1.28) * 100.0) / 100.0;

        // 2. Initial Bearing & Compass Direction
        double bearing = gpsCoordinateService.calculateBearing(
                nearestDepot.getLatitude(), nearestDepot.getLongitude(), targetLat, targetLon
        );
        String direction = gpsCoordinateService.getCompassDirection(bearing);

        // 3. Delhi Traffic Congestion Calculation
        LocalTime now = LocalTime.now();
        boolean isPeak = (now.isAfter(LocalTime.of(8, 30)) && now.isBefore(LocalTime.of(11, 30))) ||
                         (now.isAfter(LocalTime.of(17, 30)) && now.isBefore(LocalTime.of(20, 30)));

        String trafficCondition = isPeak ? "PEAK_DELHI_TRAFFIC (Arterial Congestion)" : "OFF_PEAK_FLOW (Normal Transit)";
        double avgSpeedKmh = isPeak ? 18.0 : 36.0;
        int transitMinutes = (int) Math.round((roadKm / avgSpeedKmh) * 60.0);
        int totalEtaMinutes = Math.max(5, transitMinutes + 5); // 5 min depot crew mobilization

        // 4. Vehicle Type Selection
        String vehicleType = "Rapid Pipeline Sleeve Repair Van (DJB Unit)";
        if (targetDescription != null && (targetDescription.contains("WATER_SCARCITY") || targetDescription.contains("DRY_TAP"))) {
            vehicleType = "9000L Emergency Potable Water Tanker";
        } else if (targetDescription != null && targetDescription.contains("CONTAMINATION")) {
            vehicleType = "Water Quality Testing & Flushing Unit";
        }

        // 5. Generate Turn-by-Turn Waypoints (Depot to Target)
        List<List<Double>> routeCoords = generateRouteWaypoints(
                nearestDepot.getLatitude(), nearestDepot.getLongitude(), targetLat, targetLon
        );

        // 6. Build GeoJSON LineString Feature
        Map<String, Object> routeGeoJson = new LinkedHashMap<>();
        routeGeoJson.put("type", "Feature");
        Map<String, Object> geom = new LinkedHashMap<>();
        geom.put("type", "LineString");
        geom.put("coordinates", routeCoords);
        routeGeoJson.put("geometry", geom);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("depotCode", nearestDepot.getDepotCode());
        props.put("depotName", nearestDepot.getName());
        props.put("roadDistanceKm", roadKm);
        props.put("etaMinutes", totalEtaMinutes);
        props.put("trafficCondition", trafficCondition);
        props.put("vehicleType", vehicleType);
        routeGeoJson.put("properties", props);

        return DispatchRoutePlan.builder()
                .targetLatitude(targetLat)
                .targetLongitude(targetLon)
                .targetDescription(targetDescription)
                .assignedDepot(nearestDepot)
                .geodesicDistanceKm(geodesicKm)
                .roadDistanceKm(roadKm)
                .bearingDegrees(Math.round(bearing * 10.0) / 10.0)
                .cardinalDirection(direction)
                .trafficCondition(trafficCondition)
                .estimatedMinutes(totalEtaMinutes)
                .recommendedVehicleType(vehicleType)
                .routeCoordinates(routeCoords)
                .routeGeoJson(routeGeoJson)
                .build();
    }

    /**
     * Dispatch coordination for an existing DBSCAN cluster.
     */
    public DispatchRoutePlan coordinateDispatchForCluster(Long clusterId) {
        IncidentCluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + clusterId));

        String desc = cluster.getClusterCode() + ": " +
                (cluster.getRootCauseHypothesis() != null ? cluster.getRootCauseHypothesis() : "Cluster Emergency");

        return coordinateDispatch(cluster.getCentroidLat(), cluster.getCentroidLon(), desc);
    }

    /**
     * Generates intermediate road route coordinates between depot and incident location.
     */
    private List<List<Double>> generateRouteWaypoints(double lat1, double lon1, double lat2, double lon2) {
        List<List<Double>> waypoints = new ArrayList<>();
        waypoints.add(List.of(lon1, lat1)); // Start at depot [lon, lat]

        // 4 Intermediate navigation waypoints along Delhi grid
        int segments = 5;
        for (int i = 1; i < segments; i++) {
            double fraction = (double) i / segments;
            double intermediateLat = lat1 + (lat2 - lat1) * fraction;
            double intermediateLon = lon1 + (lon2 - lon1) * fraction;

            // Add realistic road dogleg perturbation
            double jitterLat = Math.sin(fraction * Math.PI) * 0.0012;
            double jitterLon = Math.cos(fraction * Math.PI) * 0.0009;

            waypoints.add(List.of(
                    Math.round((intermediateLon + jitterLon) * 1000000.0) / 1000000.0,
                    Math.round((intermediateLat + jitterLat) * 1000000.0) / 1000000.0
            ));
        }

        waypoints.add(List.of(lon2, lat2)); // Terminate at hazard location
        return waypoints;
    }

    /**
     * Delhi Civic Landmarks and Wards Geocoder Search.
     */
    public List<Map<String, Object>> searchLandmarks(String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String q = query.toLowerCase().trim();
        List<Map<String, Object>> catalog = List.of(
                Map.of("name", "Pusa Road / Metro Pillar 118", "locality", "Karol Bagh (Ward 85)", "lat", 28.6445, "lon", 77.1950, "type", "METRO_CORRIDOR"),
                Map.of("name", "Ganga Ram Hospital Road", "locality", "Old Rajinder Nagar", "lat", 28.6385, "lon", 77.1895, "type", "HOSPITAL_ZONE"),
                Map.of("name", "Arya Samaj Road & WEA", "locality", "Karol Bagh Central", "lat", 28.6500, "lon", 77.1905, "type", "RESIDENTIAL_MARKET"),
                Map.of("name", "Lajpat Nagar Central Market", "locality", "Lajpat Nagar Part II (Ward 142)", "lat", 28.5680, "lon", 77.2430, "type", "COMMERCIAL_HUB"),
                Map.of("name", "Moolchand Flyover Junction", "locality", "Ring Road (Ward 142)", "lat", 28.5645, "lon", 77.2355, "type", "TRAFFIC_ARTERIAL"),
                Map.of("name", "Mayur Vihar Phase 1 Pocket 1", "locality", "Trunk Nallah (Ward 210)", "lat", 28.6080, "lon", 77.2980, "type", "RESIDENTIAL_DRAINAGE"),
                Map.of("name", "Noida Link Road / Mayur Vihar Flyover", "locality", "Trans-Yamuna East", "lat", 28.6140, "lon", 77.2920, "type", "HIGHWAY"),
                Map.of("name", "Rohini West Metro Station", "locality", "Sector 14 (Ward 65)", "lat", 28.7150, "lon", 77.1150, "type", "METRO_CORRIDOR"),
                Map.of("name", "Outer Ring Road Rohini Institutional", "locality", "Sector 14 (Ward 65)", "lat", 28.7180, "lon", 77.1350, "type", "INSTITUTIONAL"),
                Map.of("name", "Connaught Place Inner Circle", "locality", "Rajiv Chowk (Ward 104)", "lat", 28.6328, "lon", 77.2197, "type", "CENTRAL_BUSINESS"),
                Map.of("name", "Barakhamba Road & Tolstoy Marg", "locality", "NDMC Precinct (Ward 104)", "lat", 28.6275, "lon", 77.2245, "type", "CIVIC_PRECINCT"),
                Map.of("name", "India Gate & C-Hexagon", "locality", "Central Vista, New Delhi", "lat", 28.6129, "lon", 77.2295, "type", "NATIONAL_LANDMARK"),
                Map.of("name", "Wazirabad Water Treatment Plant", "locality", "Yamuna Basin, North Delhi", "lat", 28.7100, "lon", 77.2350, "type", "WATER_TREATMENT_PLANT"),
                Map.of("name", "Sonia Vihar Water Treatment Plant", "locality", "Yamuna East Basin", "lat", 28.7250, "lon", 77.2650, "type", "WATER_TREATMENT_PLANT")
        );

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> item : catalog) {
            String name = (String) item.get("name");
            String locality = (String) item.get("locality");
            if (name.toLowerCase().contains(q) || locality.toLowerCase().contains(q)) {
                matches.add(item);
            }
        }
        return matches;
    }
}
