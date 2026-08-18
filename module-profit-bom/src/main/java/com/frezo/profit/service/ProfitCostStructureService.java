package com.frezo.profit.service;

import com.frezo.profit.dto.request.ProfitCostCategoryRequest;
import com.frezo.profit.dto.request.ProfitEmployeeAllocationRequest;
import com.frezo.profit.dto.request.ProfitFixedOverheadRequest;
import com.frezo.profit.dto.request.ProfitSalesTargetPreviewRequest;
import com.frezo.profit.dto.response.ProfitCostCategoryDto;
import com.frezo.profit.dto.response.ProfitEmployeeAllocationDto;
import com.frezo.profit.dto.response.ProfitFixedOverheadDto;
import com.frezo.profit.dto.response.ProfitSalesTargetPreviewDto;

import java.util.List;

public interface ProfitCostStructureService {

    List<ProfitCostCategoryDto> listCategories(String marketCode);

    ProfitCostCategoryDto createCategory(ProfitCostCategoryRequest req);

    ProfitCostCategoryDto updateCategory(String id, ProfitCostCategoryRequest req);

    void deleteCategory(String id);

    List<ProfitFixedOverheadDto> listFixedOverhead(Integer year, Integer month);

    ProfitFixedOverheadDto upsertFixedOverhead(ProfitFixedOverheadRequest req);

    void deleteFixedOverhead(String id);

    List<ProfitEmployeeAllocationDto> listEmployeeAllocations();

    ProfitEmployeeAllocationDto createEmployeeAllocation(ProfitEmployeeAllocationRequest req);

    ProfitEmployeeAllocationDto updateEmployeeAllocation(String id, ProfitEmployeeAllocationRequest req);

    void deleteEmployeeAllocation(String id);

    ProfitSalesTargetPreviewDto previewSalesTarget(ProfitSalesTargetPreviewRequest req);
}
