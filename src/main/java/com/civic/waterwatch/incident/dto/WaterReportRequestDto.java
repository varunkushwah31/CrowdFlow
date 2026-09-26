package com.civic.waterwatch.incident.dto;

import com.civic.waterwatch.incident.model.IssueType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WaterReportRequestDto {
    private IssueType issueType;
    private String description;
    private Double latitude;
    private Double longitude;
    private String citizenName;
    private String citizenPhone;
    private String citizenEmail;
    private String imageUrl;
}
