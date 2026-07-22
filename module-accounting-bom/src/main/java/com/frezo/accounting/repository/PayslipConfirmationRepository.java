package com.frezo.accounting.repository;

import com.frezo.accounting.entity.PayslipConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipConfirmationRepository extends JpaRepository<PayslipConfirmation, String> {

    Optional<PayslipConfirmation> findByPayrollId(String payrollId);

    List<PayslipConfirmation> findByPersonIdOrderByConfirmedAtDesc(String personId);
}
