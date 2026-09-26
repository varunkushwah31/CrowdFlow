package com.civic.waterwatch.incident.dto;

import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WaterReportResponseDto {
    private Long id;
    private String reportCode;
    private IssueType issueType;
    private String issueTypeName;
    private String category;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String address;
    private String neighborhood;
    private Integer wardNumber;
    private String wardName;
    private String municipalBody;
    private String citizenName;
    private String deviceModel;
    private LocalDateTime reportedAt;
    private LocalDateTime capturedAt;
    private Long clusterId;
    private ReportStatus status;
    private String statusDescription;
    private String statusNotes;

    public static WaterReportResponseDto fromEntity(WaterReport r) {
        return WaterReportResponseDto.builder()
                .id(r.getId())
                .reportCode(r.getReportCode())
                .issueType(r.getIssueType())
                .issueTypeName(r.getIssueType() != null ? r.getIssueType().getDisplayName() : "")
                .category(r.getIssueType() != null ? r.getIssueType().getCategory() : "")
                .description(r.getDescription())
                .imageUrl(r.getImageUrl())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .address(r.getAddress())
                .neighborhood(r.getNeighborhood())
                .wardNumber(r.getWardNumber())
                .wardName(r.getWardName())
                .municipalBody(r.getMunicipalBody())
                .citizenName(r.getCitizenName())
                .deviceModel(r.getDeviceModel())
                .reportedAt(r.getReportedAt())
                .capturedAt(r.getCapturedAt())
                .clusterId(r.getClusterId())
                .status(r.getStatus())
                .statusDescription(r.getStatus() != null ? r.getStatus().getDescription() : "")
                .statusNotes(r.getStatusNotes())
                .build();
    }
}
