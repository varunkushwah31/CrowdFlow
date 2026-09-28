package com.civic.waterwatch.geo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Detailed representation of GPS coordinates in Indian Civic & Geodetic systems.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinateValidationResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Double latitude;
    private Double longitude;
    private boolean validForIndia;
    private boolean inDelhiNcr;
    private String ddFormat;
    private String dmsFormat;
    private String utmZone;
    private Double utmEasting;
    private Double utmNorthing;
    private String indianGridRef;
    private String plusCode;
    private String resolvedAddress;
    private Integer wardNumber;
    private String wardName;
    private String message;
}
