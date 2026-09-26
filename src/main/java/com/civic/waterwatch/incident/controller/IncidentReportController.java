package com.civic.waterwatch.incident.controller;

import com.civic.waterwatch.incident.dto.ExifMetadataResult;
import com.civic.waterwatch.incident.dto.GeoJsonFeatureCollectionDto;
import com.civic.waterwatch.incident.dto.WaterReportRequestDto;
import com.civic.waterwatch.incident.dto.WaterReportResponseDto;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.service.ExifParserService;
import com.civic.waterwatch.incident.service.IncidentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Incident Reporting (India Civic)", description = "Endpoints for submitting water issue reports, extracting camera EXIF data, and geospatial visualization across Indian cities")
public class IncidentReportController {

    private final IncidentReportService reportService;
    private final ExifParserService exifParserService;
    private final com.civic.waterwatch.incident.service.LiveTrackingService liveTrackingService;

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
    public ResponseEntity<WaterReportResponseDto> submitReportJson(@RequestBody WaterReportRequestDto dto) {
        WaterReportResponseDto created = reportService.submitReport(dto, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/extract-exif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview camera EXIF GPS coordinates and timestamp from an image before saving")
    public ResponseEntity<ExifMetadataResult> extractExifPreview(@RequestPart("file") MultipartFile file) {
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
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/track/{reportCode}/live-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time Server-Sent Events (SSE) for live field progress updates")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamLiveTracking(@PathVariable String reportCode) {
        return liveTrackingService.subscribeLiveUpdates(reportCode);
    }

    @PostMapping("/track/{reportCode}/feedback")
    @Operation(summary = "Submit citizen satisfaction rating (1-5 stars) and field comments once grievance is resolved")
    public ResponseEntity<com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto> submitFeedback(
            @PathVariable String reportCode,
            @RequestParam("rating") int rating,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        com.civic.waterwatch.incident.dto.LiveGrievanceTrackingDto updated =
                liveTrackingService.submitCitizenFeedback(reportCode, rating, comment);
        return ResponseEntity.ok(updated);
    }
}
