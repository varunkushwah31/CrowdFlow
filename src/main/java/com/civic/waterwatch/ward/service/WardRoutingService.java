package com.civic.waterwatch.ward.service;

import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.repository.MunicipalWardRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WardRoutingService {

    private final MunicipalWardRepository wardRepository;

    @PostConstruct
    public void initDefaultWards() {
        if (wardRepository.count() == 0) {
            log.info("Seeding Indian municipal wards (Delhi Jal Board & MCD civic spatial envelopes)...");
            
            wardRepository.saveAll(List.of(
                    new MunicipalWard(
                            85, "Ward 85 - Karol Bagh & Pusa Road", "Central Zone",
                            "Delhi Jal Board (DJB)", "Delhi / India", "110005",
                            "Shri Alok Sharma", "Executive Engineer (EE - Water Central)",
                            "ee.water.karolbagh@delhijalboard.nic.in", "+91 98110 23412", "1916 (DJB Toll-Free)",
                            "http://localhost:8085/api/municipal/mock-webhook/ward85",
                            28.6300, 28.6600, 77.1800, 77.2150,
                            28.6450, 77.1950
                    ),
                    new MunicipalWard(
                            142, "Ward 142 - Lajpat Nagar & Moolchand", "South Zone II",
                            "Delhi Jal Board (DJB)", "Delhi / India", "110024",
                            "Smt. Sunita Rao", "Assistant Executive Engineer (AEE - South)",
                            "aee.water.south2@delhijalboard.nic.in", "+91 98711 54321", "1916 (DJB Toll-Free)",
                            "http://localhost:8085/api/municipal/mock-webhook/ward142",
                            28.5550, 28.5850, 77.2250, 77.2550,
                            28.5700, 77.2400
                    ),
                    new MunicipalWard(
                            65, "Ward 65 - Rohini Sector 14 & Outer Ring Road", "North-West Zone",
                            "Delhi Jal Board (DJB)", "Delhi / India", "110085",
                            "Er. Rajesh Verma", "Executive Engineer (EE - North West)",
                            "ee.water.nw@delhijalboard.nic.in", "+91 99100 87654", "1916 (DJB Toll-Free)",
                            "http://localhost:8085/api/municipal/mock-webhook/ward65",
                            28.7000, 28.7350, 77.1100, 77.1450,
                            28.7150, 77.1300
                    ),
                    new MunicipalWard(
                            210, "Ward 210 - Mayur Vihar Phase 1 & Nallah Basin", "Trans-Yamuna East Zone",
                            "Municipal Corporation of Delhi (MCD) / DJB", "Delhi / India", "110091",
                            "Er. Mohammad Farhan", "AEE (Stormwater Drainage & Sewerage)",
                            "aee.drainage.east@delhijalboard.nic.in", "+91 98188 34567", "1916 (DJB Helpline)",
                            "http://localhost:8085/api/municipal/mock-webhook/ward210",
                            28.5900, 28.6250, 77.2800, 77.3150,
                            28.6080, 77.2980
                    ),
                    new MunicipalWard(
                            104, "Ward 104 - Connaught Place & Barakhamba", "NDMC Civic Zone",
                            "New Delhi Municipal Council (NDMC)", "Delhi / India", "110001",
                            "Er. Vikramaditya Sen", "Superintending Engineer (Civil & Water)",
                            "water.grievance@ndmc.gov.in", "+91 11 2336 5432", "1533 (NDMC Helpline)",
                            "http://localhost:8080/api/municipal/mock-webhook/ward104",
                            28.6180, 28.6450, 77.2050, 77.2350,
                            28.6315, 77.2195
                    )
            ));
            log.info("Successfully seeded 5 Indian Municipal Wards with emergency contacts and spatial envelopes.");
        }
    }

    public List<MunicipalWard> getAllWards() {
        return wardRepository.findAll();
    }

    public Optional<MunicipalWard> findWardByNumber(Integer wardNumber) {
        return wardRepository.findByWardNumber(wardNumber);
    }

    public MunicipalWard routeToWard(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return null;
        }

        List<MunicipalWard> wards = wardRepository.findAll();
        if (wards.isEmpty()) {
            return null;
        }

        // 1. Direct containment check
        for (MunicipalWard ward : wards) {
            if (ward.contains(lat, lon)) {
                return ward;
            }
        }

        // 2. Fallback: Closest ward centroid
        return wards.stream()
                .min(Comparator.comparingDouble(w -> w.distanceToCenter(lat, lon)))
                .orElse(wards.getFirst());
    }
}
