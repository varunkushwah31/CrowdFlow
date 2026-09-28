package com.civic.waterwatch.geo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Municipal Emergency Response Depot & Jal Board Maintenance Base.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyDepot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String depotCode;
    private String category;
    private Double latitude;
    private Double longitude;
    private String address;
    private String nodalOfficer;
    private String phone;
    private String radioChannel;
    private int waterTankerCount;
    private int repairCrewCount;
    private String status;
}
