package com.civic.waterwatch.incident.controller;

import com.civic.waterwatch.exception.FileValidationException;
import com.civic.waterwatch.exception.MediaStorageException;
import com.civic.waterwatch.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/media")
@Slf4j
@Tag(name = "Media Storage (India Civic)", description = "Static file serving and media validation with directory traversal protection")
public class MediaStorageController {

    @Value("${waterwatch.storage.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Serve uploaded citizen incident evidence photo or video")
    public ResponseEntity<Resource> serveMediaFile(@PathVariable String filename) {
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = baseDir.resolve(filename).normalize();

        // Security check: Guard against Path Traversal (e.g. ../../etc/passwd)
        if (!file.startsWith(baseDir)) {
            log.warn("Directory traversal attempt detected for filename: '{}'", filename);
            throw new FileValidationException(filename, "Directory traversal path sequence forbidden");
        }

        Resource resource;
        try {
            resource = new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new MediaStorageException("Malformed URI for media file: " + filename, e);
        }

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Media file", "filename", filename);
        }

        String contentType = "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (lower.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (lower.endsWith(".mp4")) {
            contentType = "video/mp4";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }
}
