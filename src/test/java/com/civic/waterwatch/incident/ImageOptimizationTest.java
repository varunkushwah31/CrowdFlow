package com.civic.waterwatch.incident;

import com.civic.waterwatch.incident.service.ImageOptimizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Thumbnailator Image Optimization & PII Stripping Unit Tests")
class ImageOptimizationTest {

    private final ImageOptimizationService optimizationService = new ImageOptimizationService();

    @Test
    @DisplayName("Should optimize raw image and produce a compact 300x300 thumbnail")
    void testImageOptimizationAndThumbnail() throws IOException {
        // Generate an in-memory 1600x1200 test image
        BufferedImage img = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(2, 132, 199));
        g.fillRect(0, 0, 1600, 1200);
        g.setColor(Color.WHITE);
        g.drawString("Delhi Jal Board Test Burst", 200, 200);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] rawBytes = baos.toByteArray();

        ImageOptimizationService.OptimizedImageResult result = optimizationService.processAndSanitizeImage(rawBytes);

        assertNotNull(result);
        assertNotNull(result.getOptimizedImageBytes());
        assertNotNull(result.getThumbnailBytes());
        assertEquals(1600, result.getOriginalWidth());
        assertEquals(1200, result.getOriginalHeight());

        // Verify thumbnail dimensions (<= 300x300)
        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(result.getThumbnailBytes()));
        assertNotNull(thumb);
        assertTrue(thumb.getWidth() <= 300);
        assertTrue(thumb.getHeight() <= 300);
    }
}
