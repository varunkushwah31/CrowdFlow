package com.civic.waterwatch.incident.service;

import com.civic.waterwatch.incident.dto.ExifMetadataResult;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ExifParserService {

    private static final DateTimeFormatter EXIF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public ExifMetadataResult extractMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ExifMetadataResult.withoutGps("No file provided");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return extractMetadata(inputStream, file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("Failed to extract EXIF metadata from file: {}", file.getOriginalFilename(), e);
            return ExifMetadataResult.withoutGps("Extraction failed: " + e.getMessage());
        }
    }

    public ExifMetadataResult extractMetadata(InputStream inputStream, String fileName) {
        ExifMetadataResult result = new ExifMetadataResult();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            // 1. Extract GPS Information
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    result.setHasGps(true);
                    result.setLatitude(geoLocation.getLatitude());
                    result.setLongitude(geoLocation.getLongitude());

                    String altitudeStr = gpsDirectory.getString(GpsDirectory.TAG_ALTITUDE);
                    if (altitudeStr != null) {
                        result.setAltitude(altitudeStr);
                    }
                }
            }

            // 2. Extract Timestamp via java.time API using static ExifDirectoryBase access
            ExifSubIFDDirectory subIfdDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfdDirectory != null) {
                String dateStr = subIfdDirectory.getString(ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
                if (dateStr != null && !dateStr.isBlank()) {
                    try {
                        result.setCapturedAt(LocalDateTime.parse(dateStr.trim(), EXIF_DATE_FORMATTER));
                    } catch (Exception parseEx) {
                        log.debug("Could not parse EXIF date string '{}': {}", dateStr, parseEx.getMessage());
                    }
                }
            }

            // 3. Extract Device & Camera Info using static ExifDirectoryBase access
            ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Directory != null) {
                result.setCameraMake(ifd0Directory.getString(ExifDirectoryBase.TAG_MAKE));
                result.setCameraModel(ifd0Directory.getString(ExifDirectoryBase.TAG_MODEL));
            }

            if (result.isHasGps()) {
                result.setExtractionStatus("SUCCESS: Embedded GPS extracted successfully");
            } else {
                result.setExtractionStatus("WARNING: No GPS coordinates found in image EXIF tags");
            }

            return result;
        } catch (Exception e) {
            log.info("EXIF parsing non-fatal notice for {}: {}", fileName, e.getMessage());
            result.setHasGps(false);
            result.setExtractionStatus("No EXIF data readable: " + e.getMessage());
            return result;
        }
    }
}
