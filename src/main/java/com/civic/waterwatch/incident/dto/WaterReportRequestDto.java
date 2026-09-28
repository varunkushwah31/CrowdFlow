package com.civic.waterwatch.incident.dto;

import com.civic.waterwatch.incident.model.IssueType;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Data Transfer Object for citizen water report submission with comprehensive input validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WaterReportRequestDto {

    @NotNull(message = "Issue type is required")
    private IssueType issueType;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    @Size(max = 100, message = "Citizen name must not exceed 100 characters")
    private String citizenName;

    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$|^$", message = "Invalid Indian mobile phone number. Format: +91XXXXXXXXXX or 10 digits")
    private String citizenPhone;

    @Email(message = "Citizen email must be a valid email address format")
    private String citizenEmail;

    private String imageUrl;
}
