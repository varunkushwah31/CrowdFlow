package com.civic.waterwatch.incident.controller;

import com.civic.waterwatch.exception.FileValidationException;
import com.civic.waterwatch.exception.RateLimitExceededException;
import com.civic.waterwatch.exception.ReportNotFoundException;
import com.civic.waterwatch.incident.dto.ExifMetadataResult;
import com.civic.waterwatch.incident.dto.GeoJsonFeatureCollectionDto;
import com.civic.waterwatch.incident.dto.WaterReportRequestDto;
import com.civic.waterwatch.incident.dto.WaterReportResponseDto;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.service.ExifParserService;
import com.civic.waterwatch.incident.service.IncidentReportService;
import com.civic.waterwatch.incident.service.LiveTrackingService;
import com.civic.waterwatch.redis.RedisRateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Incident Reporting (India Civic)", description = "Endpoints for submitting water issue reports, extracting camera EXIF data, and geospatial visualization across Indian cities")
public class IncidentReportController {

    private final IncidentReportService reportService;
    private final ExifParserService exifParserService;
    private final LiveTrackingService liveTrackingService;
    private final RedisRateLimiterService rateLimiterService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a water issue report with media upload and automated camera EXIF extraction")
    public ResponseEntity<WaterReportResponseDto> submitReportWithFile(
            @RequestParam("issueType") IssueType issueType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "citizenName", required = false) String citizenName,
            @RequestParam(value = "citizenPhone", required = false) String citizenPhone,
            @RequestParam(value = "citizenEmail", required = false) String citizenEmail,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        String clientKey = (citizenPhone != null && !citizenPhone.isBlank()) ? citizenPhone : "anonymous-client";
        if (rateLimiterService != null && !rateLimiterService.isAllowed(clientKey, 30, 60)) {
            throw new RateLimitExceededException(clientKey, 30, 60);
        }

        WaterReportRequestDto dto = WaterReportRequestDto.builder()
                .issueType(issueType)
                .description(description)
                .latitude(latitude)
                .longitude(longitude)
                .citizenName(citizenName)
                .citizenPhone(citizenPhone)
                .citizenEmail(citizenEmail)
                .build();

        WaterReportResponseDto created = reportService.submitReport(dto, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit a water issue report via raw JSON payload")
    public ResponseEntity<WaterReportResponseDto> submitReportJson(@Valid @RequestBody WaterReportRequestDto dto) {
        String clientKey = (dto != null && dto.getCitizenPhone() != null && !dto.getCitizenPhone().isBlank())
                ? dto.getCitizenPhone() : "anonymous-client";
        if (rateLimiterService != null && !rateLimiterService.isAllowed(clientKey, 30, 60)) {
            throw new RateLimitExceededException(clientKey, 30, 60);
        }

        WaterReportResponseDto created = reportService.submitReport(dto, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/extract-exif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview camera EXIF GPS coordinates and timestamp from an image before saving")
    public ResponseEntity<ExifMetadataResult> extractExifPreview(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("file", "Uploaded image file is empty or missing");
        }
        ExifMetadataResult result = exifParserService.extractMetadata(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "Get list of all Indian water incident reports")
    public ResponseEntity<List<WaterReportResponseDto>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/geojson")
    @Operation(summary = "Get GeoJSON FeatureCollection of all reports for Leaflet map plotting")
    public ResponseEntity<GeoJsonFeatureCollectionDto> getReportsGeoJson() {
        return ResponseEntity.ok(reportService.getReportsGeoJson());
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get coordinates with severity intensity weights for thermal heatmap rendering")
    public ResponseEntity<List<List<Double>>> getHeatmapData() {
        return ResponseEntity.ok(reportService.getHeatmapPoints());
    }

    @GetMapping("/by-cluster/{clusterId}")
    @Operation(summary = "Get all reports linked to a specific incident cluster")
    public ResponseEntity<List<WaterReportResponseDto>> getReportsByCluster(@PathVariable Long clusterId) {
        return ResponseEntity.ok(reportService.getReportsByCluster(clusterId));
    }

    @GetMapping("/track/{reportCode}")
    @Operation(summary = "Track citizen water grievance live by unique tracking code (e.g. IND-H2O-1686)")
    public ResponseEntity<com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto> trackReport(@PathVariable String reportCode) {
        return liveTrackingService.getLiveTracking(reportCode)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ReportNotFoundException(reportCode));
    }

    @GetMapping(value = "/track/{reportCode}/live-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time Server-Sent Events (SSE) for live field progress updates")
    public SseEmitter streamLiveTracking(@PathVariable String reportCode) {
        return liveTrackingService.subscribeLiveUpdates(reportCode);
    }

    @PostMapping("/track/{reportCode}/feedback")
    @Operation(summary = "Submit citizen satisfaction rating (1-5 stars) and field comments once grievance is resolved")
    public ResponseEntity<com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto> submitFeedback(
            @PathVariable String reportCode,
            @RequestParam("rating") int rating,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Citizen satisfaction rating must be between 1 and 5 stars. Submitted: " + rating);
        }
        com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto updated =
                liveTrackingService.submitCitizenFeedback(reportCode, rating, comment);
        return ResponseEntity.ok(updated);
    }
}
