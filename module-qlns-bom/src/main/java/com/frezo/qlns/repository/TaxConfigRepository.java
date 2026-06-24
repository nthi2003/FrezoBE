package com.frezo.qlns.repository;

import com.frezo.qlns.entity.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxConfigRepository extends JpaRepository<TaxConfig, String> {
    List<TaxConfig> findByYearAndIsActiveTrueOrderByBracketOrderAsc(Integer year);
    Optional<TaxConfig> findByYearAndIsActiveTrue(Integer year);
}
