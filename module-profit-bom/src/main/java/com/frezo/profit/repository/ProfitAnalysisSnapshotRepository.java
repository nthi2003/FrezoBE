package com.frezo.profit.repository;

import com.frezo.profit.entity.ProfitAnalysisSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfitAnalysisSnapshotRepository extends JpaRepository<ProfitAnalysisSnapshot, String> {

    List<ProfitAnalysisSnapshot> findByYearAndMonthAndIsDeletedFalseOrderByCalculatedAtDesc(
            Integer year, Integer month);
}
