package com.civic.waterwatch.ward.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "municipal_wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MunicipalWard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ward_number", unique = true, nullable = false)
    private Integer wardNumber;

    @Column(name = "ward_name", nullable = false, length = 128)
    private String wardName;

    @Column(name = "zone_name", length = 64)
    private String zoneName;

    @Column(name = "municipal_body", length = 128)
    private String municipalBody; // Delhi Jal Board (DJB) / MCD

    @Column(name = "state", length = 64)
    private String state = "Delhi / India";

    @Column(name = "pincode", length = 16)
    private String pincode;

    @Column(name = "officer_name", length = 128)
    private String officerName;

    @Column(name = "officer_designation", length = 128)
    private String officerDesignation; // e.g. Executive Engineer (EE - Water)

    @Column(name = "contact_email", length = 128)
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "emergency_hotline", length = 64)
    private String emergencyHotline; // 1916 (Jal Board) / 1533

    @Column(name = "webhook_url", length = 512)
    private String webhookUrl;

    @Column(name = "min_lat")
    private Double minLat;

    @Column(name = "max_lat")
    private Double maxLat;

    @Column(name = "min_lon")
    private Double minLon;

    @Column(name = "max_lon")
    private Double maxLon;

    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lon")
    private Double centerLon;

    public MunicipalWard(Integer wardNumber, String wardName, String zoneName, String municipalBody,
                         String state, String pincode, String officerName, String officerDesignation,
                         String contactEmail, String contactPhone, String emergencyHotline,
                         String webhookUrl, Double minLat, Double maxLat, Double minLon, Double maxLon,
                         Double centerLat, Double centerLon) {
        this.wardNumber = wardNumber;
        this.wardName = wardName;
        this.zoneName = zoneName;
        this.municipalBody = municipalBody;
        this.state = state;
        this.pincode = pincode;
        this.officerName = officerName;
        this.officerDesignation = officerDesignation;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.emergencyHotline = emergencyHotline;
        this.webhookUrl = webhookUrl;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
    }

    public boolean contains(Double lat, Double lon) {
        if (lat == null || lon == null || minLat == null || maxLat == null || minLon == null || maxLon == null) {
            return false;
        }
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    public double distanceToCenter(Double lat, Double lon) {
        if (lat == null || lon == null || centerLat == null || centerLon == null) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(lat - centerLat);
        double dLon = Math.toRadians(lon - centerLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(centerLat)) * Math.cos(Math.toRadians(lat)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c; // meters
    }
}
