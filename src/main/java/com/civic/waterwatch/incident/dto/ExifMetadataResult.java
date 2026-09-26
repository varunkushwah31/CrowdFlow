package com.civic.waterwatch.incident.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ExifMetadataResult {
    private boolean hasGps;
    private Double latitude;
    private Double longitude;
    private LocalDateTime capturedAt;
    private String cameraMake;
    private String cameraModel;
    private String altitude;
    private String extractionStatus;

    public static ExifMetadataResult withoutGps(String reason) {
        return ExifMetadataResult.builder()
                .hasGps(false)
                .extractionStatus(reason)
                .build();
    }
}
