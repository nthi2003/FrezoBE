package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.DepreciationPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepreciationPostingRepository extends JpaRepository<DepreciationPosting, String> {

    Optional<DepreciationPosting> findByPeriodYearAndPeriodMonth(Integer year, Integer month);

    List<DepreciationPosting> findByPeriodYearAndPeriodMonthAndIsDeletedFalse(Integer year, Integer month);

    List<DepreciationPosting> findByIsDeletedFalseOrderByPeriodYearDescPeriodMonthDesc();
}
