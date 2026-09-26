package com.civic.waterwatch.clustering.repository;

import com.civic.waterwatch.clustering.model.ClusterSeverity;
import com.civic.waterwatch.clustering.model.ClusterStatus;
import com.civic.waterwatch.clustering.model.IncidentCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentClusterRepository extends JpaRepository<IncidentCluster, Long> {

    Optional<IncidentCluster> findByClusterCode(String clusterCode);

    List<IncidentCluster> findByStatus(ClusterStatus status);

    List<IncidentCluster> findBySeverity(ClusterSeverity severity);

    List<IncidentCluster> findByWardNumber(Integer wardNumber);

    @Query("SELECT c FROM IncidentCluster c WHERE c.status IN ('ACTIVE', 'ESCALATED', 'UNDER_INVESTIGATION', 'IN_PROGRESS') ORDER BY c.updatedAt DESC")
    List<IncidentCluster> findOpenClusters();

    @Query("SELECT c FROM IncidentCluster c ORDER BY c.createdAt DESC")
    List<IncidentCluster> findAllOrderByCreatedAtDesc();

    long countByStatus(ClusterStatus status);

    long countBySeverity(ClusterSeverity severity);
}
