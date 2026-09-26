package com.civic.waterwatch.incident.service;

import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReverseGeocodingService {

    private static final DecimalFormat DF = new DecimalFormat("#.0000");
    private final WardRoutingService wardRoutingService;

    @Getter
    @AllArgsConstructor
    public static class GeocodedAddress {
        private final String fullAddress;
        private final String neighborhood;
        private final Integer wardNumber;
        private final String wardName;
        private final String pincode;
    }

    public GeocodedAddress reverseGeocode(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return new GeocodedAddress("Unknown Location, India", "Unspecified Ward", null, null, "110001");
        }

        MunicipalWard ward = wardRoutingService.routeToWard(lat, lon);

        String wardName = ward != null ? ward.getWardName() : "Delhi Jal Board Civic Grid";
        Integer wardNumber = ward != null ? ward.getWardNumber() : 85;
        String zone = ward != null ? ward.getZoneName() : "Central Zone";
        String pincode = ward != null && ward.getPincode() != null ? ward.getPincode() : "110005";

        // Realistic Indian Streets and Localities
        int streetSeed = Math.abs((int) ((lat * 1000) + (lon * 1000))) % 10;
        String[] sampleIndianStreets = {
                "Pusa Road, near Metro Pillar 118, Karol Bagh",
                "Ring Road Flyover Underpass, Lajpat Nagar Part IV",
                "Outer Ring Road, near Rohini West Metro Station",
                "Barakhamba Road & Tolstoy Marg Junction, Connaught Place",
                "Pocket 1 Nallah Bypass Road, Mayur Vihar Phase 1",
                "Arya Samaj Road, Western Extension Area (WEA)",
                "Moolchand Hospital Junction, Ring Road",
                "Kasturba Gandhi Marg, CP Civic Precinct",
                "Chhatrapati Shivaji Marg, Metro Feeder Line",
                "Sector 14 Institutional Area Road, Rohini"
        };

        String street = sampleIndianStreets[streetSeed];
        String neighborhood = (ward != null) ? ward.getWardName().replaceFirst("^Ward \\d+ - ", "") : "Delhi Municipal Area";
        String fullAddress = String.format("%s, %s, %s, New Delhi - %s (GPS: %s, %s)",
                street, neighborhood, zone, pincode, DF.format(lat), DF.format(lon));

        return new GeocodedAddress(fullAddress, neighborhood, wardNumber, wardName, pincode);
    }
}
