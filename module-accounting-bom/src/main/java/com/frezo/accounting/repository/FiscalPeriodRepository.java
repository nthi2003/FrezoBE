package com.frezo.accounting.repository;

import com.frezo.accounting.entity.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, String> {

    Optional<FiscalPeriod> findByMonthAndYear(Integer month, Integer year);

    List<FiscalPeriod> findByFiscalYearIdOrderByMonthAsc(String fiscalYearId);

    List<FiscalPeriod> findByYearOrderByMonthAsc(Integer year);
}
