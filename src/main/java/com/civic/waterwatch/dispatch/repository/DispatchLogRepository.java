package com.civic.waterwatch.dispatch.repository;

import com.civic.waterwatch.dispatch.model.DispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchLogRepository extends JpaRepository<DispatchLog, Long> {
    List<DispatchLog> findByClusterId(Long clusterId);
    List<DispatchLog> findAllByOrderByDispatchedAtDesc();
}
