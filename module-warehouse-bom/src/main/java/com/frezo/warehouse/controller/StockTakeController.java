package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.dto.request.StockTakeLineRequest;
import com.frezo.warehouse.dto.request.StockTakeRequest;
import com.frezo.warehouse.dto.response.StockTakeResponse;
import com.frezo.warehouse.service.StockTakeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouse/stock-takes")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Stock Take")
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @GetMapping
    @CheckPermission(api = "/warehouse/stock-takes", action = "VIEW")
    public ApiResponse<List<StockTakeResponse>> list(@RequestParam(required = false) String warehouseId) {
        return ApiResponse.ok(stockTakeService.list(warehouseId));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/stock-takes/{id}", action = "VIEW")
    public ApiResponse<StockTakeResponse> get(@PathVariable String id) {
        return ApiResponse.ok(stockTakeService.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/warehouse/stock-takes", action = "CREATE")
    public ApiResponse<StockTakeResponse> create(@RequestBody StockTakeRequest req) {
        return ApiResponse.ok(stockTakeService.create(req));
    }

    @PostMapping("/{id}/start")
    @CheckPermission(api = "/warehouse/stock-takes/{id}/start", action = "CREATE")
    public ApiResponse<StockTakeResponse> start(@PathVariable String id) {
        return ApiResponse.ok(stockTakeService.start(id));
    }

    @PostMapping("/{id}/submit-counted")
    @CheckPermission(api = "/warehouse/stock-takes/{id}/submit-counted", action = "UPDATE")
    public ApiResponse<StockTakeResponse> submitCounted(
            @PathVariable String id, @RequestBody List<StockTakeLineRequest> lines) {
        return ApiResponse.ok(stockTakeService.submitCounted(id, lines));
    }

    @PostMapping("/{id}/post-variance")
    @CheckPermission(api = "/warehouse/stock-takes/{id}/post-variance", action = "CREATE")
    public ApiResponse<StockTakeResponse> postVariance(@PathVariable String id) {
        return ApiResponse.ok(stockTakeService.postVariance(id));
    }
}
