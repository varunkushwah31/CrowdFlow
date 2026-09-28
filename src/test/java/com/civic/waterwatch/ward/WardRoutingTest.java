package com.civic.waterwatch.ward;

import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.repository.MunicipalWardRepository;
import com.civic.waterwatch.ward.service.WardRoutingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WardRoutingTest {

    @Test
    @DisplayName("Should route Indian coordinates directly into correct Delhi Jal Board ward envelope")
    void testDirectWardRouting() {
        MunicipalWard ward85 = new MunicipalWard(
                85, "Ward 85 - Karol Bagh", "Central Zone",
                "Delhi Jal Board (DJB)", "Delhi / India", "110005",
                "Shri Alok Sharma", "EE - Water",
                "ee.karolbagh@delhijalboard.nic.in", "+91 98110 23412", "1916",
                "http://test/ward85",
                28.6300, 28.6600, 77.1800, 77.2150,
                28.6450, 77.1950
        );

        MunicipalWard ward142 = new MunicipalWard(
                142, "Ward 142 - Lajpat Nagar", "South Zone",
                "Delhi Jal Board (DJB)", "Delhi / India", "110024",
                "Smt. Sunita Rao", "AEE - South",
                "aee.lajpatnagar@delhijalboard.nic.in", "+91 98711 54321", "1916",
                "http://test/ward142",
                28.5550, 28.5850, 77.2250, 77.2550,
                28.5700, 77.2400
        );

        List<MunicipalWard> allWards = List.of(ward85, ward142);

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

        WardRoutingService service = new WardRoutingService(stubRepo);

        // Coordinate inside Ward 85 (Karol Bagh: 28.6445, 77.1950)
        MunicipalWard matched = service.routeToWard(28.6445, 77.1950);
        assertNotNull(matched);
        assertEquals(85, matched.getWardNumber());
        assertEquals("Delhi Jal Board (DJB)", matched.getMunicipalBody());

        // Coordinate inside Ward 142 (Lajpat Nagar: 28.5700, 77.2400)
        MunicipalWard matched2 = service.routeToWard(28.5700, 77.2400);
        assertNotNull(matched2);
        assertEquals(142, matched2.getWardNumber());
        assertEquals("Delhi Jal Board (DJB)", matched2.getMunicipalBody());
    }

    @Test
    @DisplayName("Should route to closest ward centroid when coordinate is slightly outside all bounding boxes")
    void testCentroidFallbackRouting() {
        MunicipalWard ward65 = new MunicipalWard(
                65, "Ward 65 - Rohini Sector 14", "North-West Zone",
                "Delhi Jal Board (DJB)", "Delhi / India", "110085",
                "Er. Rajesh Verma", "EE - North West",
                "ee.water.nw@delhijalboard.nic.in", "+91 99100 87654", "1916",
                "http://test/ward65",
                28.7000, 28.7350, 77.1100, 77.1450,
                28.7150, 77.1300
        );

        List<MunicipalWard> allWards = List.of(ward65);

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

        WardRoutingService service = new WardRoutingService(stubRepo);

        // Near Rohini Sector 14 but outside bounding box
        MunicipalWard fallback = service.routeToWard(28.7400, 77.1500);
        assertNotNull(fallback);
        assertEquals(65, fallback.getWardNumber());
    }
}
