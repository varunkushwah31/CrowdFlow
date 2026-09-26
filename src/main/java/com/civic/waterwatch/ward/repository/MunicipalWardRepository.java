package com.civic.waterwatch.ward.repository;

import com.civic.waterwatch.ward.model.MunicipalWard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MunicipalWardRepository extends JpaRepository<MunicipalWard, Long> {
    Optional<MunicipalWard> findByWardNumber(Integer wardNumber);
}
