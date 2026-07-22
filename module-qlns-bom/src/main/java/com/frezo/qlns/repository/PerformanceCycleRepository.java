package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PerformanceCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceCycleRepository extends JpaRepository<PerformanceCycle, String> {
    List<PerformanceCycle> findByIsDeletedFalseOrderByStartDateDesc();
}
