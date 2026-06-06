package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, String>, JpaSpecificationExecutor<Payroll> {
    Optional<Payroll> findByPersonIdAndMonthAndYear(String personId, Integer month, Integer year);
}
