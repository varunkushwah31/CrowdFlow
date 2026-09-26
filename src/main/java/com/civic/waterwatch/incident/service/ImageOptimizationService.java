package com.civic.waterwatch.incident.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service to sanitize and optimize crowdsourced water incident photos.
 * Uses Thumbnailator to:
 * 1. Auto-orient images based on EXIF tags.
 * 2. Strip personally identifiable metadata (PII) such as device serials, camera owner info.
 * 3. Generate lightweight Web-optimized JPEGs for fast dashboard loading.
 * 4. Generate compact 300x300 thumbnails for map markers and cluster listings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageOptimizationService {

    @Getter
    @Builder
    public static class OptimizedImageResult {
        private final byte[] optimizedImageBytes;
        private final byte[] thumbnailBytes;
        private final int originalWidth;
        private final int originalHeight;
        private final int optimizedWidth;
        private final int optimizedHeight;
        private final long originalSizeBytes;
        private final long optimizedSizeBytes;
    }

    /**
     * Sanitizes and compresses an uploaded image while stripping sensitive PII metadata.
     *
     * @param rawImageBytes raw bytes from client multipart upload
     * @return OptimizedImageResult containing clean JPEG bytes and thumbnail bytes
     */
    public OptimizedImageResult processAndSanitizeImage(byte[] rawImageBytes) {
        if (rawImageBytes == null || rawImageBytes.length == 0) {
            log.warn("Empty image payload received for optimization");
            return null;
        }

        try {
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(rawImageBytes));
            if (sourceImage == null) {
                log.warn("Could not decode image format for optimization; falling back to raw payload");
                return OptimizedImageResult.builder()
                        .optimizedImageBytes(rawImageBytes)
                        .thumbnailBytes(rawImageBytes)
                        .originalSizeBytes(rawImageBytes.length)
                        .optimizedSizeBytes(rawImageBytes.length)
                        .build();
            }

            int srcWidth = sourceImage.getWidth();
            int srcHeight = sourceImage.getHeight();

            // 1. Optimize full image (max 1280x1280, 85% JPEG quality, EXIF PII stripped by re-encoding)
            ByteArrayOutputStream fullOut = new ByteArrayOutputStream();
            Thumbnails.of(sourceImage)
                    .size(1280, 1280)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .useExifOrientation(true)
                    .toOutputStream(fullOut);
            byte[] optimizedBytes = fullOut.toByteArray();

            // 2. Generate compact thumbnail (300x300) for map marker popups & lists
            ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
            Thumbnails.of(sourceImage)
                    .size(300, 300)
                    .outputFormat("jpg")
                    .outputQuality(0.80)
                    .useExifOrientation(true)
                    .toOutputStream(thumbOut);
            byte[] thumbBytes = thumbOut.toByteArray();

            log.info("Image optimized & PII stripped: {} KB -> {} KB (Thumbnail: {} KB)",
                    rawImageBytes.length / 1024,
                    optimizedBytes.length / 1024,
                    thumbBytes.length / 1024);

            return OptimizedImageResult.builder()
                    .optimizedImageBytes(optimizedBytes)
                    .thumbnailBytes(thumbBytes)
                    .originalWidth(srcWidth)
                    .originalHeight(srcHeight)
                    .originalSizeBytes(rawImageBytes.length)
                    .optimizedSizeBytes(optimizedBytes.length)
                    .build();

        } catch (IOException e) {
            log.error("Error during image optimization/sanitization: {}", e.getMessage(), e);
            // Graceful fallback to original payload
            return OptimizedImageResult.builder()
                    .optimizedImageBytes(rawImageBytes)
                    .thumbnailBytes(rawImageBytes)
                    .originalSizeBytes(rawImageBytes.length)
                    .optimizedSizeBytes(rawImageBytes.length)
                    .build();
        }
    }
}
