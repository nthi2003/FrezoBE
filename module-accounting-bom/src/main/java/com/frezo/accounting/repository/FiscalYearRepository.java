package com.frezo.accounting.repository;

import com.frezo.accounting.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, String> {

    Optional<FiscalYear> findByCode(String code);

    boolean existsByCode(String code);

    List<FiscalYear> findByIsDeletedFalseOrderByCodeDesc();
}
