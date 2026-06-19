package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PayrollConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollConfigRepository extends JpaRepository<PayrollConfig, String> {
    Optional<PayrollConfig> findByOrgIdAndYearAndIsActiveTrue(String orgId, Integer year);
}
