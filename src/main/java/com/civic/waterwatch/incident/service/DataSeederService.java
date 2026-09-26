package com.civic.waterwatch.incident.service;

import com.civic.waterwatch.clustering.service.SpatialClusteringService;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeederService {

    private final WaterReportRepository reportRepository;
    private final SpatialClusteringService clusteringService;
    private final ReverseGeocodingService reverseGeocodingService;

    @PostConstruct
    public void seedInitialDemoData() {
        if (reportRepository.count() > 0) {
            return;
        }

        log.info("Seeding Indian civic water incident reports across Delhi Jal Board & MCD wards...");

        String burstPipeSvg = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='400' height='260' viewBox='0 0 400 260'><rect width='400' height='260' fill='%230284c7'/><text x='50%25' y='42%25' font-family='Arial' font-size='18' font-weight='bold' fill='white' dominant-baseline='middle' text-anchor='middle'>DELHI JAL BOARD FEEDER BURST</text><text x='50%25' y='58%25' font-family='Arial' font-size='12' fill='%23e0f2fe' dominant-baseline='middle' text-anchor='middle'>450mm Arterial Line Rupture - Pusa Road</text></svg>";
        String sewageSvg = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='400' height='260' viewBox='0 0 400 260'><rect width='400' height='260' fill='%2378350f'/><text x='50%25' y='42%25' font-family='Arial' font-size='18' font-weight='bold' fill='white' dominant-baseline='middle' text-anchor='middle'>SEWAGE CROSS-CONTAMINATION</text><text x='50%25' y='58%25' font-family='Arial' font-size='12' fill='%23fef3c7' dominant-baseline='middle' text-anchor='middle'>MCD Trunk Sewer Overflow into Tap Line</text></svg>";
        String leakSvg = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='400' height='260' viewBox='0 0 400 260'><rect width='400' height='260' fill='%230369a1'/><text x='50%25' y='42%25' font-family='Arial' font-size='18' font-weight='bold' fill='white' dominant-baseline='middle' text-anchor='middle'>UNDERGROUND PIPE LEAKAGE</text><text x='50%25' y='58%25' font-family='Arial' font-size='12' fill='%23e0f2fe' dominant-baseline='middle' text-anchor='middle'>KG Marg Ductile Joint Seepage</text></svg>";

        List<ReportSeed> seeds = List.of(
                // Cluster 1: Major Feeder Burst on Pusa Road, Karol Bagh (Ward 85 - Delhi Jal Board)
                new ReportSeed(IssueType.BURST_PIPE, "Major rupture on 450mm ductile iron transmission line from Wazirabad WTP. Water shooting 3 meters high on Pusa Road near Metro Pillar 118.",
                        28.6445, 77.1950, burstPipeSvg, "Rajesh Malhotra", "r.malhotra@karolbagh.org", "+91 98110 54321", "Samsung Galaxy S24 Ultra", 45),
                new ReportSeed(IssueType.SEVERE_WATERLOGGING, "Pusa Road intersection submerged under 2 feet of water. Traffic paralyzed towards Karol Bagh market and Metro station.",
                        28.6450, 77.1955, burstPipeSvg, "Pooja Singhania", "p.singhania@gmail.com", "+91 98101 23456", "iPhone 15 Pro", 40),
                new ReportSeed(IssueType.LOW_PRESSURE, "Complete water pressure collapse in residential pipelines across Arya Samaj Road and WEA block.",
                        28.6440, 77.1945, leakSvg, "Vikas Aggarwal", "vikas.aggarwal@rediffmail.com", "+91 98712 34567", "Google Pixel 8", 35),
                new ReportSeed(IssueType.SEVERE_WATERLOGGING, "Pressurized floodwater entering commercial basements near Karol Bagh Metro Gate 2.",
                        28.6448, 77.1952, burstPipeSvg, "Kavita Mehra", "kavita.m@outlook.in", "+91 99100 98765", "OnePlus 12", 25),
                new ReportSeed(IssueType.BURST_PIPE, "Road asphalt cracked and water violently gushing out near Ganga Ram Hospital road intersection.",
                        28.6453, 77.1948, burstPipeSvg, "Harish Chawla", "harish.c@gmail.com", "+91 98111 87654", "iPhone 14", 15),

                // Cluster 2: Sewage Contamination in Mayur Vihar Phase 1 (Ward 210 - MCD / DJB)
                new ReportSeed(IssueType.CONTAMINATION, "Tap water in Pocket 1 is visibly yellowish-brown with foul sewage stench. Completely undrinkable.",
                        28.6080, 77.2980, sewageSvg, "Ananya Sen", "ananya.sen@gmail.com", "+91 98188 12345", "iPhone 13", 110),
                new ReportSeed(IssueType.OPEN_SEWAGE, "MCD trunk sewer manhole overflowing with black effluent right into residential drain near Pocket 1 gate.",
                        28.6085, 77.2985, sewageSvg, "Pradeep Nambiar", "p.nambiar@yahoo.co.in", "+91 98734 56789", "Samsung S23", 95),
                new ReportSeed(IssueType.DRAINAGE_OVERFLOW, "Stormwater nallah completely choked with plastic debris; backing up into apartment lanes.",
                        28.6078, 77.2978, sewageSvg, "Deepa Krishnan", "deepa.k@gmail.com", "+91 99112 34567", "Pixel 7a", 85),
                new ReportSeed(IssueType.CONTAMINATION, "Multiple children from block complaining of stomach distress after drinking filtered municipal water.",
                        28.6082, 77.2982, sewageSvg, "Dr. Sanjay Gupta", "dr.gupta@delhihealth.org", "+91 98100 11223", "iPhone 15", 60),

                // Cluster 3: Water Scarcity / Dry Taps in Lajpat Nagar Part IV (Ward 142 - DJB)
                new ReportSeed(IssueType.WATER_SCARCITY, "No municipal water supply for 48 hours across Lajpat Nagar Part IV. DJB tanker helpline 1916 busy.",
                        28.5700, 77.2400, leakSvg, "Sunil Grover", "sunil.grover@gmail.com", "+91 98102 33445", "OnePlus 11", 240),
                new ReportSeed(IssueType.LOW_PRESSURE, "Pressure collapsed completely at 6 AM distribution cycle. Overhead water tanks cannot be filled.",
                        28.5705, 77.2408, leakSvg, "Meenakshi Sundaram", "meenakshi.s@gmail.com", "+91 98711 22334", "iPhone 12", 210),
                new ReportSeed(IssueType.WATER_SCARCITY, "Third consecutive day with dry municipal pipeline in Block C. Residents relying on costly private tankers.",
                        28.5695, 77.2395, leakSvg, "Gaurav Kapoor", "gaurav.k@rediffmail.com", "+91 99103 44556", "Galaxy S24", 180),

                // Cluster 4: Joint Seepage in Connaught Place (Ward 104 - NDMC Civic Zone)
                new ReportSeed(IssueType.LEAKAGE, "Constant stream of water bubbling up from sidewalk joint on Kasturba Gandhi Marg for 3 days.",
                        28.6315, 77.2195, leakSvg, "Nitin Saxena", "nitin.saxena@ndmc-civic.in", "+91 11 2334 1122", "iPhone 14 Pro", 300),
                new ReportSeed(IssueType.LEAKAGE, "Road foundation depression forming near Tolstoy Marg junction due to underground ductile line joint leak.",
                        28.6320, 77.2200, leakSvg, "Rohit Verma", "rohit.v@cp-delhi.org", "+91 98114 55667", "Galaxy S22", 260),
                new ReportSeed(IssueType.LEAKAGE, "Clean potable water flowing into street gutter from cracked collar joint on service pipe.",
                        28.6312, 77.2190, leakSvg, "Simran Kaur", "simran.k@gmail.com", "+91 98105 66778", "Pixel 8 Pro", 220)
        );

        for (ReportSeed s : seeds) {
            WaterReport report = new WaterReport();
            report.setReportCode("IND-H2O-" + (1000 + (long) (Math.random() * 9000)));
            report.setIssueType(s.issueType());
            report.setDescription(s.description());
            report.setLatitude(s.lat());
            report.setLongitude(s.lon());
            report.setImageUrl(s.imageUrl());
            report.setCitizenName(s.citizenName());
            report.setCitizenEmail(s.citizenEmail());
            report.setCitizenPhone(s.citizenPhone());
            report.setDeviceModel(s.deviceModel());
            report.setReportedAt(LocalDateTime.now().minusMinutes(s.minutesAgo()));
            report.setCapturedAt(report.getReportedAt().minusMinutes(5));
            report.setStatus(ReportStatus.SUBMITTED);
            report.setMunicipalBody("Delhi Jal Board / MCD");

            ReverseGeocodingService.GeocodedAddress addr = reverseGeocodingService.reverseGeocode(s.lat(), s.lon());
            report.setAddress(addr.getFullAddress());
            report.setNeighborhood(addr.getNeighborhood());
            report.setWardNumber(addr.getWardNumber());
            report.setWardName(addr.getWardName());

            reportRepository.save(report);
        }

        log.info("Seeded {} Indian water reports. Running initial DBSCAN clustering...", seeds.size());
        SpatialClusteringService.ClusteringRunSummary summary = clusteringService.runClustering();
        log.info("Initial clustering ready: {} Indian civic clusters formed, {} citizen reports grouped.",
                summary.getClustersFound(), summary.getReportsClustered());
    }

    private record ReportSeed(IssueType issueType, String description, double lat, double lon,
                              String imageUrl, String citizenName, String citizenEmail,
                              String citizenPhone, String deviceModel, int minutesAgo) {}
}
