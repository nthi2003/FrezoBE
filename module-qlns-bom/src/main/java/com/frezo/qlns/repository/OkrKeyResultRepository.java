package com.frezo.qlns.repository;

import com.frezo.qlns.entity.OkrKeyResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OkrKeyResultRepository extends JpaRepository<OkrKeyResult, String> {
    List<OkrKeyResult> findByOkrIdAndIsDeletedFalseOrderBySortOrderAsc(String okrId);
}
