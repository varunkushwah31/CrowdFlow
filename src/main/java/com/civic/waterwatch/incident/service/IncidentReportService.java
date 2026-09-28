package com.civic.waterwatch.incident.service;

import com.civic.waterwatch.incident.dto.ExifMetadataResult;
import com.civic.waterwatch.incident.dto.GeoJsonFeatureCollectionDto;
import com.civic.waterwatch.incident.dto.WaterReportRequestDto;
import com.civic.waterwatch.incident.dto.WaterReportResponseDto;
import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.redis.RedisGeoSpatialService;
import com.civic.waterwatch.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentReportService {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final WaterReportRepository reportRepository;
    private final ExifParserService exifParserService;
    private final ReverseGeocodingService reverseGeocodingService;
    private final ImageOptimizationService imageOptimizationService;
    private final ObjectStorageService objectStorageService;
    private final RedisGeoSpatialService redisGeoSpatialService;

    @Value("${waterwatch.storage.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${waterwatch.geo.default-lat:28.6320}")
    private double defaultLat;

    @Value("${waterwatch.geo.default-lon:77.2105}")
    private double defaultLon;

    private record MediaProcessingResult(
            Double latitude,
            Double longitude,
            LocalDateTime capturedAt,
            String deviceModel,
            String storedImageUrl
    ) {}

    @Transactional
    @CacheEvict(value = "heatmap", allEntries = true)
    public WaterReportResponseDto submitReport(WaterReportRequestDto dto, MultipartFile file) {
        WaterReport report = new WaterReport();
        report.setReportCode(generateReportCode());
        report.setIssueType(dto.getIssueType() != null ? dto.getIssueType() : IssueType.OTHER);
        report.setDescription(dto.getDescription());
        report.setCitizenName(dto.getCitizenName());
        report.setCitizenPhone(dto.getCitizenPhone());
        report.setCitizenEmail(dto.getCitizenEmail());
        report.setReportedAt(LocalDateTime.now(IST_ZONE));
        report.setStatus(ReportStatus.SUBMITTED);
        report.setMunicipalBody("Delhi Jal Board / MCD");

        Double finalLat = dto.getLatitude();
        Double finalLon = dto.getLongitude();
        LocalDateTime capturedAt = null;
        String deviceModel = null;

        MediaProcessingResult mediaResult = processMediaUpload(file);
        if (mediaResult != null) {
            if (mediaResult.latitude() != null && mediaResult.longitude() != null) {
                finalLat = mediaResult.latitude();
                finalLon = mediaResult.longitude();
            }
            capturedAt = mediaResult.capturedAt();
            deviceModel = mediaResult.deviceModel();
            if (mediaResult.storedImageUrl() != null) {
                report.setImageUrl(mediaResult.storedImageUrl());
            }
        } else if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            report.setImageUrl(dto.getImageUrl());
        }

        if (finalLat == null || finalLon == null) {
            finalLat = defaultLat;
            finalLon = defaultLon;
        }

        report.setLatitude(finalLat);
        report.setLongitude(finalLon);
        report.setCapturedAt(capturedAt != null ? capturedAt : LocalDateTime.now(IST_ZONE));
        report.setDeviceModel(deviceModel != null ? deviceModel : "Citizen Mobile Client (India)");

        ReverseGeocodingService.GeocodedAddress address = reverseGeocodingService.reverseGeocode(finalLat, finalLon);
        report.setAddress(address.getFullAddress());
        report.setNeighborhood(address.getNeighborhood());
        report.setWardNumber(address.getWardNumber());
        report.setWardName(address.getWardName());

        report = reportRepository.save(report);
        log.info("Persisted new Indian civic report: {} at [{}, {}] in {}",
                report.getReportCode(), report.getLatitude(), report.getLongitude(), report.getWardName());

        if (redisGeoSpatialService != null && report.getLatitude() != null && report.getLongitude() != null) {
            redisGeoSpatialService.indexReportLocation(report.getReportCode(), report.getLatitude(), report.getLongitude());
        }

        return WaterReportResponseDto.fromEntity(report);
    }

    private String generateReportCode() {
        return "IND-H2O-" + ThreadLocalRandom.current().nextLong(1000, 10000);
    }

    private MediaProcessingResult processMediaUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Double exifLat = null;
        Double exifLon = null;
        LocalDateTime capturedAt = null;
        String deviceModel = null;
        String storedUrl = null;

        try {
            // 1. Extract EXIF metadata (GPS, Timestamp, Device) before PII stripping
            ExifMetadataResult exifResult = exifParserService.extractMetadata(file);
            if (exifResult.isHasGps()) {
                exifLat = exifResult.getLatitude();
                exifLon = exifResult.getLongitude();
                log.info("EXIF GPS extracted from Indian citizen upload: Lat {}, Lon {}", exifLat, exifLon);
            }
            if (exifResult.getCapturedAt() != null) {
                capturedAt = exifResult.getCapturedAt();
            }
            if (exifResult.getCameraModel() != null) {
                deviceModel = (exifResult.getCameraMake() != null ? exifResult.getCameraMake() + " " : "") + exifResult.getCameraModel();
            }

            // 2. Sanitize & compress image (auto-orient, strip PII, generate clean JPEG)
            byte[] rawBytes = file.getBytes();
            ImageOptimizationService.OptimizedImageResult optResult = imageOptimizationService.processAndSanitizeImage(rawBytes);
            byte[] bytesToStore = (optResult != null && optResult.getOptimizedImageBytes() != null)
                    ? optResult.getOptimizedImageBytes()
                    : rawBytes;

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "report_evidence.jpg";
            String cleanName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
            if (!cleanName.toLowerCase().endsWith(".jpg") && !cleanName.toLowerCase().endsWith(".jpeg") && !cleanName.toLowerCase().endsWith(".png")) {
                cleanName += ".jpg";
            }

            // 3. Store via unified ObjectStorageService (S3 / MinIO / Local)
            storedUrl = objectStorageService.storeMedia(cleanName, bytesToStore, "image/jpeg");
        } catch (Exception e) {
            log.warn("Failed to store/optimize uploaded media file: {}", e.getMessage());
        }

        return new MediaProcessingResult(exifLat, exifLon, capturedAt, deviceModel, storedUrl);
    }

    public List<WaterReportResponseDto> getAllReports() {
        return reportRepository.findAllOrderByReportedAtDesc().stream()
                .map(WaterReportResponseDto::fromEntity)
                .toList();
    }

    public List<WaterReportResponseDto> getReportsByCluster(Long clusterId) {
        return reportRepository.findByClusterId(clusterId).stream()
                .map(WaterReportResponseDto::fromEntity)
                .toList();
    }

    public Optional<WaterReportResponseDto> getReportByCode(String reportCode) {
        return reportRepository.findByReportCode(reportCode).map(WaterReportResponseDto::fromEntity);
    }

    public GeoJsonFeatureCollectionDto getReportsGeoJson() {
        List<WaterReport> reports = reportRepository.findAll();
        GeoJsonFeatureCollectionDto featureCollection = new GeoJsonFeatureCollectionDto();

        for (WaterReport r : reports) {
            if (r.getLatitude() != null && r.getLongitude() != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("id", r.getId());
                props.put("reportCode", r.getReportCode());
                props.put("issueType", r.getIssueType().name());
                props.put("issueLabel", r.getIssueType().getDisplayName());
                props.put("category", r.getIssueType().getCategory());
                props.put("severityWeight", r.getIssueType().getSeverityWeight());
                props.put("status", r.getStatus().name());
                props.put("statusLabel", r.getStatus().getDescription());
                props.put("address", r.getAddress());
                props.put("wardNumber", r.getWardNumber());
                props.put("wardName", r.getWardName());
                props.put("municipalBody", r.getMunicipalBody());
                props.put("imageUrl", r.getImageUrl());
                props.put("clusterId", r.getClusterId());
                props.put("reportedAt", r.getReportedAt().toString());

                featureCollection.addFeature(
                        new GeoJsonFeatureCollectionDto.Geometry("Point", List.of(r.getLongitude(), r.getLatitude())),
                        props
                );
            }
        }
        return featureCollection;
    }

    @Cacheable(value = "heatmap", key = "'points'")
    public List<List<Double>> getHeatmapPoints() {
        List<WaterReport> reports = reportRepository.findAll();
        List<List<Double>> points = new ArrayList<>();
        for (WaterReport r : reports) {
            if (r.getLatitude() != null && r.getLongitude() != null) {
                double intensity = 0.3 + (r.getIssueType().getSeverityWeight() * 0.14);
                points.add(List.of(r.getLatitude(), r.getLongitude(), intensity));
            }
        }
        return points;
    }
}
