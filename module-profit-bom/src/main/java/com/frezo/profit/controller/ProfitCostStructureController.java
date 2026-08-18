package com.frezo.profit.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.profit.dto.request.ProfitCostCategoryRequest;
import com.frezo.profit.dto.request.ProfitEmployeeAllocationRequest;
import com.frezo.profit.dto.request.ProfitFixedOverheadRequest;
import com.frezo.profit.dto.request.ProfitSalesTargetPreviewRequest;
import com.frezo.profit.service.ProfitCostStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profit/cost-structure")
@RequiredArgsConstructor
@Tag(name = "Profit — Cost Structure", description = "Cost centers, fixed overhead, employee allocation")
public class ProfitCostStructureController {

    private final ProfitCostStructureService costStructureService;

    @GetMapping("/categories")
    @CheckPermission(api = "/profit/cost-structure/categories", action = "VIEW")
    public ApiResponse<?> listCategories(@RequestParam(required = false) String marketCode) {
        return ApiResponse.success(costStructureService.listCategories(marketCode));
    }

    @PostMapping("/categories")
    @CheckPermission(api = "/profit/cost-structure/categories", action = "CREATE")
    public ApiResponse<?> createCategory(@Valid @RequestBody ProfitCostCategoryRequest req) {
        return ApiResponse.success(costStructureService.createCategory(req));
    }

    @PutMapping("/categories/{id}")
    @CheckPermission(api = "/profit/cost-structure/categories/{id}", action = "UPDATE")
    public ApiResponse<?> updateCategory(@PathVariable String id, @Valid @RequestBody ProfitCostCategoryRequest req) {
        return ApiResponse.success(costStructureService.updateCategory(id, req));
    }

    @DeleteMapping("/categories/{id}")
    @CheckPermission(api = "/profit/cost-structure/categories/{id}", action = "DELETE")
    public ApiResponse<?> deleteCategory(@PathVariable String id) {
        costStructureService.deleteCategory(id);
        return ApiResponse.success("Đã xóa danh mục chi phí");
    }

    @GetMapping("/fixed-overhead")
    @CheckPermission(api = "/profit/cost-structure/fixed-overhead", action = "VIEW")
    public ApiResponse<?> listFixedOverhead(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.success(costStructureService.listFixedOverhead(year, month));
    }

    @PostMapping("/fixed-overhead")
    @CheckPermission(api = "/profit/cost-structure/fixed-overhead", action = "CREATE")
    @Operation(summary = "Upsert chi phí cố định tháng")
    public ApiResponse<?> upsertFixedOverhead(@Valid @RequestBody ProfitFixedOverheadRequest req) {
        return ApiResponse.success(costStructureService.upsertFixedOverhead(req));
    }

    @DeleteMapping("/fixed-overhead/{id}")
    @CheckPermission(api = "/profit/cost-structure/fixed-overhead/{id}", action = "DELETE")
    public ApiResponse<?> deleteFixedOverhead(@PathVariable String id) {
        costStructureService.deleteFixedOverhead(id);
        return ApiResponse.success("Đã xóa chi phí cố định");
    }

    @GetMapping("/employee-allocations")
    @CheckPermission(api = "/profit/cost-structure/employee-allocations", action = "VIEW")
    public ApiResponse<?> listEmployeeAllocations() {
        return ApiResponse.success(costStructureService.listEmployeeAllocations());
    }

    @PostMapping("/employee-allocations")
    @CheckPermission(api = "/profit/cost-structure/employee-allocations", action = "CREATE")
    public ApiResponse<?> createEmployeeAllocation(@Valid @RequestBody ProfitEmployeeAllocationRequest req) {
        return ApiResponse.success(costStructureService.createEmployeeAllocation(req));
    }

    @PutMapping("/employee-allocations/{id}")
    @CheckPermission(api = "/profit/cost-structure/employee-allocations/{id}", action = "UPDATE")
    public ApiResponse<?> updateEmployeeAllocation(
            @PathVariable String id, @Valid @RequestBody ProfitEmployeeAllocationRequest req) {
        return ApiResponse.success(costStructureService.updateEmployeeAllocation(id, req));
    }

    @DeleteMapping("/employee-allocations/{id}")
    @CheckPermission(api = "/profit/cost-structure/employee-allocations/{id}", action = "DELETE")
    public ApiResponse<?> deleteEmployeeAllocation(@PathVariable String id) {
        costStructureService.deleteEmployeeAllocation(id);
        return ApiResponse.success("Đã xóa phân bổ nhân sự");
    }

    @PostMapping("/preview-sales-target")
    @CheckPermission(api = "/profit/cost-structure/employee-allocations", action = "VIEW")
    @Operation(summary = "Xem trước KPI doanh thu từ lương 3P + HĐ")
    public ApiResponse<?> previewSalesTarget(@RequestBody ProfitSalesTargetPreviewRequest req) {
        return ApiResponse.success(costStructureService.previewSalesTarget(req));
    }
}
