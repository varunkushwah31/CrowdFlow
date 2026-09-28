package com.civic.waterwatch.geo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Live GPS tracking telemetry for Delhi Jal Board Emergency Water Tankers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterTankerUnit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String vehicleNumber;
    private int capacityLiters;
    private String depotCode;
    private String driverName;
    private String driverPhone;
    private String status;
    private Double latitude;
    private Double longitude;
    private Double headingDegrees;
    private String destinationWard;
}
