package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when a municipal ward cannot be found by number or coordinates.
 */
public class WardNotFoundException extends WaterWatchException {

    public WardNotFoundException(Integer wardNumber) {
        super(
                "Municipal ward not found for ward number: " + wardNumber,
                HttpStatus.NOT_FOUND,
                "WARD_NOT_FOUND",
                Map.of("wardNumber", wardNumber)
        );
    }

    public WardNotFoundException(Double lat, Double lon) {
        super(
                String.format("No municipal ward resolved for coordinates [%.6f, %.6f]", lat, lon),
                HttpStatus.NOT_FOUND,
                "WARD_NOT_FOUND",
                Map.of("latitude", lat, "longitude", lon)
        );
    }
}
