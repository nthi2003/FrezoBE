package com.frezo.profit.repository;

import com.frezo.profit.entity.ProfitCostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfitCostCategoryRepository extends JpaRepository<ProfitCostCategory, String> {

    List<ProfitCostCategory> findByIsDeletedFalseOrderBySortOrderAscNameAsc();

    List<ProfitCostCategory> findByMarketCodeAndIsDeletedFalseOrderBySortOrderAsc(String marketCode);

    Optional<ProfitCostCategory> findByCodeAndMarketCodeAndIsDeletedFalse(
            com.frezo.profit.common.CostCategoryCode code, String marketCode);
}
