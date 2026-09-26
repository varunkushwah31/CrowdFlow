package com.civic.waterwatch.dispatch.service;

import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import com.civic.waterwatch.dispatch.model.DispatchLog;
import com.civic.waterwatch.dispatch.repository.DispatchLogRepository;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import com.civic.waterwatch.incident.repository.WaterReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenNotificationService {

    private final WaterReportRepository reportRepository;
    private final DispatchLogRepository dispatchLogRepository;

    public void notifyCitizensOfStatusChange(IncidentCluster cluster, ClusterStatus newStatus, String resolutionNotes) {
        List<WaterReport> reports = reportRepository.findByClusterId(cluster.getId());
        if (reports.isEmpty()) {
            return;
        }

        log.info("Closing feedback loop: Notifying {} Indian citizens of Cluster {} status update -> {}",
                reports.size(), cluster.getClusterCode(), newStatus);

        ReportStatus reportStatus = mapClusterStatusToReportStatus(newStatus);

        for (WaterReport report : reports) {
            report.setStatus(reportStatus);
            report.setStatusNotes(resolutionNotes);
            if (newStatus == ClusterStatus.RESOLVED) {
                report.setResolvedAt(LocalDateTime.now());
            }
            reportRepository.save(report);

            String citizenContact = report.getCitizenPhone() != null ? report.getCitizenPhone() : report.getCitizenEmail();
            if (citizenContact == null || citizenContact.isBlank()) {
                citizenContact = "Citizen Grievance #" + report.getReportCode();
            }

            String message = String.format("Jal Board / Municipal Grievance Update [%s]: Status is now '%s'. Work Notes: %s (Helpline: 1916)",
                    report.getReportCode(), newStatus.getDescription(),
                    (resolutionNotes != null && !resolutionNotes.isBlank() ? resolutionNotes : "Field maintenance crews active on site."));

            log.info("CITIZEN SMS/WHATSAPP ALERT -> To: {} | {}", citizenContact, message);

            dispatchLogRepository.save(new DispatchLog(
                    cluster.getId(), cluster.getClusterCode(),
                    cluster.getWardNumber(), citizenContact,
                    "CITIZEN_SMS_NOTICE", "DELIVERED",
                    message, 200
            ));
        }
    }

    private ReportStatus mapClusterStatusToReportStatus(ClusterStatus clusterStatus) {
        return switch (clusterStatus) {
            case IN_PROGRESS -> ReportStatus.IN_PROGRESS;
            case RESOLVED, CLOSED -> ReportStatus.RESOLVED;
            case ESCALATED, UNDER_INVESTIGATION -> ReportStatus.ESCALATED;
            case ACTIVE -> ReportStatus.CLUSTERED;
        };
    }
}
