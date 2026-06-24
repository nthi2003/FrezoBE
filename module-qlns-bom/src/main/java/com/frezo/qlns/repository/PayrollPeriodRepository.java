package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, String>, JpaSpecificationExecutor<PayrollPeriod> {
    Optional<PayrollPeriod> findByOrgIdAndMonthAndYear(String orgId, Integer month, Integer year);
}
