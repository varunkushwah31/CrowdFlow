package com.civic.waterwatch.reporting.service;

import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.clustering.repository.IncidentClusterRepository;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.service.WardRoutingService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    private final TemplateEngine templateEngine;
    private final IncidentClusterRepository clusterRepository;
    private final WaterReportRepository reportRepository;
    private final WardRoutingService wardRoutingService;

    @Value("${waterwatch.storage.reports-dir:./generated-reports}")
    private String reportsDir;

    @Getter
    @AllArgsConstructor
    public static class PhotoEvidenceItem {
        private final String reportCode;
        private final String issueType;
        private final String address;
        private final String imageUrl;
    }

    public byte[] generateClusterReport(Long clusterId) {
        IncidentCluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new com.civic.waterwatch.exception.ClusterNotFoundException(clusterId));

        List<WaterReport> reports = reportRepository.findByClusterId(clusterId);
        MunicipalWard ward = null;
        if (cluster.getWardNumber() != null) {
            ward = wardRoutingService.findWardByNumber(cluster.getWardNumber()).orElse(null);
        }

        List<PhotoEvidenceItem> photoEvidence = new ArrayList<>();
        String placeholderSvg = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='200' viewBox='0 0 300 200'><rect width='300' height='200' fill='%23e2e8f0'/><text x='50%25' y='50%25' font-family='Arial' font-size='13' fill='%2364748b' dominant-baseline='middle' text-anchor='middle'>Jal Board Field Inspection Photo</text></svg>";

        for (WaterReport r : reports) {
            String imgUrl = (r.getImageUrl() != null && !r.getImageUrl().isBlank()) ? r.getImageUrl() : placeholderSvg;
            photoEvidence.add(new PhotoEvidenceItem(
                    r.getReportCode(),
                    r.getIssueType().getDisplayName(),
                    r.getAddress() != null ? r.getAddress() : "Location on Jal Board Record",
                    imgUrl
            ));
            if (photoEvidence.size() >= 3) {
                break;
            }
        }

        if (photoEvidence.isEmpty()) {
            photoEvidence.add(new PhotoEvidenceItem(cluster.getClusterCode(), "Hazard Area", "GPS Centroid Grid Area", placeholderSvg));
        }

        Context context = new Context();
        context.setVariable("cluster", cluster);
        context.setVariable("ward", ward);
        context.setVariable("reports", reports);
        context.setVariable("photoEvidence", photoEvidence);
        context.setVariable("generatedAt", LocalDateTime.now());

        String renderedHtml = templateEngine.process("incident-report", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(renderedHtml, new File(".").toURI().toString());
            builder.toStream(os);
            builder.run();

            byte[] pdfBytes = os.toByteArray();

            Path outputDirectory = Paths.get(reportsDir);
            if (!Files.exists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
            }
            String filename = "dossier-" + cluster.getClusterCode() + ".pdf";
            Path targetFile = outputDirectory.resolve(filename);
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                fos.write(pdfBytes);
            }

            cluster.setDossierPdfPath(targetFile.toAbsolutePath().toString());
            clusterRepository.save(cluster);

            log.info("Compiled official Indian Municipal PDF Dossier for Cluster {} ({} bytes) -> {}",
                    cluster.getClusterCode(), pdfBytes.length, targetFile.toAbsolutePath());

            return pdfBytes;
        } catch (com.civic.waterwatch.exception.ClusterNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate PDF for cluster {}: {}", cluster.getClusterCode(), e.getMessage(), e);
            throw new com.civic.waterwatch.exception.PdfGenerationException(clusterId, e);
        }
    }
}
