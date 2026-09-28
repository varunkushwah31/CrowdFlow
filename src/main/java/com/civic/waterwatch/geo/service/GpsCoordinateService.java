package com.civic.waterwatch.geo.service;

import com.civic.waterwatch.geo.model.CoordinateValidationResult;
import com.civic.waterwatch.incident.service.ReverseGeocodingService;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Enterprise GPS and Geodetic Coordination Engine calibrated for India and Delhi NCR.
 * Provides coordinate transformations (DD, DMS, UTM Zone 43N, Indian Grid, Plus Codes),
 * sovereign boundary verification, and high-precision geodesic calculations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GpsCoordinateService {

    private final ReverseGeocodingService reverseGeocodingService;
    private final WardRoutingService wardRoutingService;

    // Sovereign India Terrestrial Bounding Envelope
    public static final double INDIA_MIN_LAT = 8.0668;
    public static final double INDIA_MAX_LAT = 37.1000;
    public static final double INDIA_MIN_LON = 68.1166;
    public static final double INDIA_MAX_LON = 97.4167;

    // Delhi National Capital Region (NCR) Civic Operations Envelope
    public static final double NCR_MIN_LAT = 28.3500;
    public static final double NCR_MAX_LAT = 28.9500;
    public static final double NCR_MIN_LON = 76.8000;
    public static final double NCR_MAX_LON = 77.4500;

    private static final double EARTH_RADIUS_METERS = 6371008.8;

    /**
     * Validates and converts raw GPS coordinates into all major Indian civic and tactical formats.
     */
    public CoordinateValidationResult validateAndEnrich(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return CoordinateValidationResult.builder()
                    .validForIndia(false)
                    .inDelhiNcr(false)
                    .message("GPS coordinates are missing or null")
                    .build();
        }

        boolean validIndia = (latitude >= INDIA_MIN_LAT && latitude <= INDIA_MAX_LAT) &&
                             (longitude >= INDIA_MIN_LON && longitude <= INDIA_MAX_LON);

        boolean inNcr = (latitude >= NCR_MIN_LAT && latitude <= NCR_MAX_LAT) &&
                        (longitude >= NCR_MIN_LON && longitude <= NCR_MAX_LON);

        // 1. Decimal Degrees (DD)
        String dd = String.format(Locale.US, "%.6f° %s, %.6f° %s",
                Math.abs(latitude), latitude >= 0 ? "N" : "S",
                Math.abs(longitude), longitude >= 0 ? "E" : "W");

        // 2. Degrees Minutes Seconds (DMS)
        String dms = toDms(latitude, longitude);

        // 3. Universal Transverse Mercator (UTM Zone 43N for Northern/Central India, Central Meridian 75°E)
        int utmZoneNum = (int) Math.floor((longitude + 180.0) / 6.0) + 1;
        String utmZone = "UTM Zone " + utmZoneNum + "N (WGS-84)";
        double[] utm = computeUtm(latitude, longitude, utmZoneNum);
        double utmEasting = Math.round(utm[0] * 100.0) / 100.0;
        double utmNorthing = Math.round(utm[1] * 100.0) / 100.0;

        // 4. Indian Grid Reference
        String indianGrid = computeIndianGridRef(utmZoneNum, utmEasting, utmNorthing);

        // 5. Plus Code (Open Location Code algorithm for India)
        String plusCode = computePlusCode(latitude, longitude);

        // 6. Reverse Geocoding and Ward Affiliation
        ReverseGeocodingService.GeocodedAddress addr = reverseGeocodingService.reverseGeocode(latitude, longitude);
        MunicipalWard ward = wardRoutingService.routeToWard(latitude, longitude);

        String message = validIndia
                ? (inNcr ? "Verified within Delhi NCR civic grid" : "Verified within India boundary (outside Delhi NCR)")
                : "WARNING: Coordinates are outside sovereign Indian territory";

        return CoordinateValidationResult.builder()
                .latitude(latitude)
                .longitude(longitude)
                .validForIndia(validIndia)
                .inDelhiNcr(inNcr)
                .ddFormat(dd)
                .dmsFormat(dms)
                .utmZone(utmZone)
                .utmEasting(utmEasting)
                .utmNorthing(utmNorthing)
                .indianGridRef(indianGrid)
                .plusCode(plusCode)
                .resolvedAddress(addr != null ? addr.getFullAddress() : null)
                .wardNumber(ward != null ? ward.getWardNumber() : null)
                .wardName(ward != null ? ward.getWardName() : null)
                .message(message)
                .build();
    }

    /**
     * Converts coordinates to Degrees Minutes Seconds (DMS).
     */
    public String toDms(double lat, double lon) {
        String latDms = toSingleDms(lat, "N", "S");
        String lonDms = toSingleDms(lon, "E", "W");
        return latDms + ", " + lonDms;
    }

    private String toSingleDms(double val, String pos, String neg) {
        String dir = val >= 0 ? pos : neg;
        double abs = Math.abs(val);
        int deg = (int) abs;
        double minFrac = (abs - deg) * 60;
        int min = (int) minFrac;
        double sec = (minFrac - min) * 60;
        return String.format(Locale.US, "%d° %d' %.2f\" %s", deg, min, sec, dir);
    }

    /**
     * Computes UTM Easting and Northing (WGS-84 ellipsoid).
     */
    public double[] computeUtm(double lat, double lon, int zone) {
        double a = 6378137.0; // WGS-84 semi-major axis
        double f = 1 / 298.257223563; // flattening
        double k0 = 0.9996; // UTM scale factor
        double e2 = 2 * f - f * f; // first eccentricity squared
        double ePrime2 = e2 / (1 - e2);

        double centralLon = ((zone - 1) * 6 - 180 + 3);
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double radCentralLon = Math.toRadians(centralLon);

        double N = a / Math.sqrt(1 - e2 * Math.sin(radLat) * Math.sin(radLat));
        double T = Math.tan(radLat) * Math.tan(radLat);
        double C = ePrime2 * Math.cos(radLat) * Math.cos(radLat);
        double A = Math.cos(radLat) * (radLon - radCentralLon);

        double M = a * ((1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256) * radLat
                - (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2 * e2 * e2 / 1024) * Math.sin(2 * radLat)
                + (15 * e2 * e2 / 256 + 45 * e2 * e2 * e2 / 1024) * Math.sin(4 * radLat)
                - (35 * e2 * e2 * e2 / 3072) * Math.sin(6 * radLat));

        double easting = k0 * N * (A + (1 - T + C) * Math.pow(A, 3) / 6.0
                + (5 - 18 * T + T * T + 72 * C - 58 * ePrime2) * Math.pow(A, 5) / 120.0) + 500000.0;

        double northing = k0 * (M + N * Math.tan(radLat) * (A * A / 2.0
                + (5 - T + 9 * C + 4 * C * C) * Math.pow(A, 4) / 24.0
                + (61 - 58 * T + T * T + 600 * C - 330 * ePrime2) * Math.pow(A, 6) / 720.0));

        return new double[]{easting, northing};
    }

    /**
     * Computes Indian Military & Civic Grid coordinate notation.
     */
    public String computeIndianGridRef(int zone, double easting, double northing) {
        int e100k = (int) (easting / 100000.0);
        int n100k = (int) (northing / 100000.0);

        char colLetter = (char) ('A' + (e100k % 8));
        char rowLetter = (char) ('A' + (n100k % 20));

        int eRem = (int) (easting % 100000) / 100;
        int nRem = (int) (northing % 100000) / 100;

        return String.format(Locale.US, "%dR-%c%c-%03d%03d", zone, colLetter, rowLetter, eRem, nRem);
    }

    /**
     * Computes 8-character Open Location Code (Plus Code) for precise civic field dispatch.
     */
    public String computePlusCode(double lat, double lon) {
        String alphabet = "23456789CFGHJMPQRVWX";
        double adjLat = lat + 90.0;
        double adjLon = lon + 180.0;

        StringBuilder sb = new StringBuilder();
        double latStep = 20.0;
        double lonStep = 20.0;

        for (int i = 0; i < 4; i++) {
            int latIdx = (int) (adjLat / latStep);
            int lonIdx = (int) (adjLon / lonStep);
            latIdx = Math.clamp(latIdx, 0, 19);
            lonIdx = Math.clamp(lonIdx, 0, 19);

            sb.append(alphabet.charAt(latIdx));
            sb.append(alphabet.charAt(lonIdx));

            adjLat -= latIdx * latStep;
            adjLon -= lonIdx * lonStep;

            latStep /= 20.0;
            lonStep /= 20.0;

            if (i == 3) {
                sb.append("+");
            }
        }

        // Add 2 refinement characters
        int rLat = (int) (adjLat / (latStep / 20.0));
        int rLon = (int) (adjLon / (lonStep / 20.0));
        sb.append(alphabet.charAt(Math.clamp(rLat, 0, 19)));
        sb.append(alphabet.charAt(Math.clamp(rLon, 0, 19)));

        return sb.toString();
    }

    /**
     * Computes geodesic distance in kilometers between two GPS coordinates using high-precision Haversine.
     */
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (EARTH_RADIUS_METERS * c) / 1000.0;
    }

    /**
     * Computes initial compass bearing from Point A to Point B in degrees (0° to 360°).
     */
    public double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) -
                Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

        double theta = Math.toDegrees(Math.atan2(y, x));
        return (theta + 360.0) % 360.0;
    }

    /**
     * Returns compass cardinal direction from bearing degrees.
     */
    public String getCompassDirection(double bearing) {
        String[] directions = {"North (N)", "North-East (NE)", "East (E)", "South-East (SE)",
                "South (S)", "South-West (SW)", "West (W)", "North-West (NW)"};
        int index = (int) Math.round(((bearing % 360) / 45.0)) % 8;
        return directions[index];
    }
}
