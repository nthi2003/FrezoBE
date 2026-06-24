package com.frezo.qlns.repository;

import com.frezo.qlns.entity.InsuranceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceConfigRepository extends JpaRepository<InsuranceConfig, String> {
    Optional<InsuranceConfig> findByYearAndIsActiveTrue(Integer year);
}
