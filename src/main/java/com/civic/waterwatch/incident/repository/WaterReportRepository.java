package com.civic.waterwatch.incident.repository;

import com.civic.waterwatch.incident.model.IssueType;
import com.civic.waterwatch.incident.model.ReportStatus;
import com.civic.waterwatch.incident.model.WaterReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterReportRepository extends JpaRepository<WaterReport, Long> {

    Optional<WaterReport> findByReportCode(String reportCode);

    List<WaterReport> findByClusterId(Long clusterId);

    List<WaterReport> findByClusterIdIn(List<Long> clusterIds);

    List<WaterReport> findByStatus(ReportStatus status);

    List<WaterReport> findByIssueType(IssueType issueType);

    List<WaterReport> findByWardNumber(Integer wardNumber);

    @Query("SELECT r FROM WaterReport r WHERE r.clusterId IS NULL AND r.status = :status AND r.reportedAt >= :since")
    List<WaterReport> findUnclusteredReportsSince(
            @Param("status") ReportStatus status,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT r FROM WaterReport r WHERE r.latitude BETWEEN :minLat AND :maxLat AND r.longitude BETWEEN :minLon AND :maxLon")
    List<WaterReport> findInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    @Query("SELECT r FROM WaterReport r ORDER BY r.reportedAt DESC")
    List<WaterReport> findAllOrderByReportedAtDesc();

    long countByStatus(ReportStatus status);

    long countByClusterIdIsNotNull();
}
