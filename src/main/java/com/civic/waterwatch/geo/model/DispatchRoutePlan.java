package com.civic.waterwatch.geo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Tactical Dispatch & Navigation Route Plan connecting Municipal Depots to Incident Clusters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchRoutePlan implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Double targetLatitude;
    private Double targetLongitude;
    private String targetDescription;
    private EmergencyDepot assignedDepot;
    private Double geodesicDistanceKm;
    private Double roadDistanceKm;
    private Double bearingDegrees;
    private String cardinalDirection;
    private String trafficCondition;
    private int estimatedMinutes;
    private String recommendedVehicleType;
    private List<List<Double>> routeCoordinates;
    private Map<String, Object> routeGeoJson;
}
