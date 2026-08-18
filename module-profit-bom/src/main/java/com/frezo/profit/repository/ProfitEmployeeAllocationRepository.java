package com.frezo.profit.repository;

import com.frezo.profit.common.CostCategoryCode;
import com.frezo.profit.entity.ProfitEmployeeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfitEmployeeAllocationRepository extends JpaRepository<ProfitEmployeeAllocation, String> {

    List<ProfitEmployeeAllocation> findByIsDeletedFalseOrderByCostCategoryCodeAsc();

    List<ProfitEmployeeAllocation> findByCostCategoryCodeAndIsDeletedFalseAndActiveTrue(
            CostCategoryCode costCategoryCode);

    List<ProfitEmployeeAllocation> findByIsDeletedFalseAndActiveTrue();
}
