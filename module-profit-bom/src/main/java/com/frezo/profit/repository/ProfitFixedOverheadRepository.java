package com.frezo.profit.repository;

import com.frezo.profit.entity.ProfitFixedOverhead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfitFixedOverheadRepository extends JpaRepository<ProfitFixedOverhead, String> {

    List<ProfitFixedOverhead> findByYearAndMonthAndIsDeletedFalse(Integer year, Integer month);

    Optional<ProfitFixedOverhead> findByCategoryIdAndYearAndMonthAndIsDeletedFalse(
            String categoryId, Integer year, Integer month);
}
