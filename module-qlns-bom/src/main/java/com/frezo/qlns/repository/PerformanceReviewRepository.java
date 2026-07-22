package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, String> {
    List<PerformanceReview> findByIsDeletedFalseOrderByCreatedDateDesc();
    List<PerformanceReview> findByCycleIdAndIsDeletedFalse(String cycleId);
    List<PerformanceReview> findByPersonIdAndIsDeletedFalse(String personId);
}
