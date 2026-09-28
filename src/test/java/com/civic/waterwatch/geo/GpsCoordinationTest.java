package com.civic.waterwatch.geo;

import com.civic.waterwatch.geo.model.CoordinateValidationResult;
import com.civic.waterwatch.geo.model.DispatchRoutePlan;
import com.civic.waterwatch.geo.model.EmergencyDepot;
import com.civic.waterwatch.geo.model.WaterTankerUnit;
import com.civic.waterwatch.geo.service.GpsCoordinateService;
import com.civic.waterwatch.geo.service.MunicipalCoordinationService;
import com.civic.waterwatch.incident.service.ReverseGeocodingService;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.repository.MunicipalWardRepository;
import com.civic.waterwatch.ward.service.WardRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GpsCoordinationTest {

    private GpsCoordinateService gpsCoordinateService;
    private MunicipalCoordinationService municipalCoordinationService;

    @BeforeEach
    void setUp() {
        MunicipalWard ward85 = new MunicipalWard(
                85, "Ward 85 - Karol Bagh", "Central Zone",
                "Delhi Jal Board (DJB)", "Delhi / India", "110005",
                "Shri Alok Sharma", "EE - Water",
                "ee.karolbagh@delhijalboard.nic.in", "+91 98110 23412", "1916",
                "http://test/ward85",
                28.6300, 28.6600, 77.1800, 77.2150,
                28.6450, 77.1950
        );

        List<MunicipalWard> allWards = List.of(ward85);

        MunicipalWardRepository stubRepo = (MunicipalWardRepository) Proxy.newProxyInstance(
                MunicipalWardRepository.class.getClassLoader(),
                new Class<?>[]{MunicipalWardRepository.class},
                (proxy, method, args) -> {
                    if ("findByWardNumber".equals(method.getName())) {
                        Integer wardNumber = (Integer) args[0];
                        return allWards.stream().filter(w -> w.getWardNumber().equals(wardNumber)).findFirst();
                    }
                    if ("findAll".equals(method.getName())) {
                        return allWards;
                    }
                    if ("count".equals(method.getName())) {
                        return (long) allWards.size();
                    }
                    return null;
                }
        );

        WardRoutingService wardRoutingService = new WardRoutingService(stubRepo);
        ReverseGeocodingService reverseGeocodingService = new ReverseGeocodingService(wardRoutingService);
        gpsCoordinateService = new GpsCoordinateService(reverseGeocodingService, wardRoutingService);

        municipalCoordinationService = new MunicipalCoordinationService(gpsCoordinateService, null);
    }

    @Test
    @DisplayName("Should validate Delhi coordinates and produce accurate UTM, DMS, and Plus Code")
    void testDelhiCoordinateValidation() {
        double lat = 28.6445;
        double lon = 77.1950;

        CoordinateValidationResult result = gpsCoordinateService.validateAndEnrich(lat, lon);

        assertTrue(result.isValidForIndia(), "Should be inside sovereign India boundaries");
        assertTrue(result.isInDelhiNcr(), "Should be recognized inside Delhi NCR operational envelope");
        assertNotNull(result.getDdFormat());
        assertNotNull(result.getDmsFormat());
        assertTrue(result.getDmsFormat().contains("28° 38'"));
        assertTrue(result.getUtmZone().contains("43N"));
        assertTrue(result.getUtmEasting() > 0);
        assertTrue(result.getUtmNorthing() > 0);
        assertNotNull(result.getIndianGridRef());
        assertNotNull(result.getPlusCode());
        assertTrue(result.getPlusCode().contains("+"), "Plus code should include '+' separator");
    }

    @Test
    @DisplayName("Should detect coordinates outside India")
    void testOutsideIndiaCoordinates() {
        CoordinateValidationResult result = gpsCoordinateService.validateAndEnrich(51.5074, -0.1278); // London
        assertFalse(result.isValidForIndia());
        assertFalse(result.isInDelhiNcr());
        assertTrue(result.getMessage().contains("outside"));
    }

    @Test
    @DisplayName("Should compute accurate geodesic distance and bearing across Delhi")
    void testGeodesicDistanceAndBearing() {
        // From Karol Bagh (28.6445, 77.1950) to Lajpat Nagar (28.5710, 77.2415)
        double distKm = gpsCoordinateService.calculateDistanceKm(28.6445, 77.1950, 28.5710, 77.2415);
        assertTrue(distKm > 8.0 && distKm < 13.0, "Distance across central to south Delhi should be ~9-11 km, was: " + distKm);

        double bearing = gpsCoordinateService.calculateBearing(28.6445, 77.1950, 28.5710, 77.2415);
        assertTrue(bearing > 120.0 && bearing < 170.0, "Bearing heading south-southeast should be around 140-155 degrees, was: " + bearing);

        String compass = gpsCoordinateService.getCompassDirection(bearing);
        assertTrue(compass.contains("South") || compass.contains("East"));
    }

    @Test
    @DisplayName("Should coordinate emergency dispatch to nearest DJB depot")
    void testCoordinateDispatch() {
        // Incident at Pusa Road, Karol Bagh (28.6445, 77.1950)
        DispatchRoutePlan plan = municipalCoordinationService.coordinateDispatch(
                28.6445, 77.1950, "MAJOR_LEAKAGE: Main water pipe burst near Metro Pillar 118"
        );

        assertNotNull(plan);
        assertNotNull(plan.getAssignedDepot());
        assertEquals("PUSA-BOOSTER-85", plan.getAssignedDepot().getDepotCode());
        assertTrue(plan.getRoadDistanceKm() > 0);
        assertTrue(plan.getEstimatedMinutes() >= 5);
        assertNotNull(plan.getTrafficCondition());
        assertNotNull(plan.getRecommendedVehicleType());
        assertNotNull(plan.getRouteCoordinates());
        assertFalse(plan.getRouteCoordinates().isEmpty());
        assertNotNull(plan.getRouteGeoJson());
    }

    @Test
    @DisplayName("Should list all 6 master emergency depots and 5 active water tankers")
    void testDepotsAndTankers() {
        List<EmergencyDepot> depots = municipalCoordinationService.getAllDepots();
        assertEquals(6, depots.size(), "Should have 6 configured emergency bases across Delhi");

        List<WaterTankerUnit> tankers = municipalCoordinationService.getActiveTankers();
        assertEquals(5, tankers.size(), "Should have 5 registered DJB tankers");
        assertTrue(tankers.stream().anyMatch(t -> t.getVehicleNumber().equals("DL-1M-4512")));
    }

    @Test
    @DisplayName("Should geocode and search Delhi landmarks correctly")
    void testLandmarkSearch() {
        List<Map<String, Object>> pusaResults = municipalCoordinationService.searchLandmarks("Pusa");
        assertFalse(pusaResults.isEmpty());
        assertTrue(pusaResults.getFirst().get("name").toString().contains("Pusa"));

        List<Map<String, Object>> connaughtResults = municipalCoordinationService.searchLandmarks("Connaught");
        assertFalse(connaughtResults.isEmpty());
        assertTrue(connaughtResults.getFirst().get("name").toString().contains("Connaught Place"));
    }
}
