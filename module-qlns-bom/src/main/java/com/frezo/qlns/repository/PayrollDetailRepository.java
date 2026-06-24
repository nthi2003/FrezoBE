package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PayrollDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollDetailRepository extends JpaRepository<PayrollDetail, String> {
    List<PayrollDetail> findByPayrollIdOrderBySortOrderAsc(String payrollId);
    void deleteByPayrollId(String payrollId);
}
