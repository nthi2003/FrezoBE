package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.JobPositionRequest;
import com.frezo.qlns.dto.response.CategoryUsageResponse;
import com.frezo.qlns.dto.response.JobPositionResponse;
import com.frezo.qlns.service.JobPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/job-position")
@RequiredArgsConstructor
@Tag(name = "HR - Vị trí công việc")
public class JobPositionController {

    private final JobPositionService jobPositionService;

    @GetMapping
    @CheckPermission(api = "/qlns/job-position", action = "VIEW")
    public ApiResponse<List<JobPositionResponse>> list() {
        return ApiResponse.ok(jobPositionService.list());
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/job-position/{id}", action = "VIEW")
    public ApiResponse<JobPositionResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(jobPositionService.getById(id));
    }

    @PostMapping
    @CheckPermission(api = "/qlns/job-position", action = "CREATE")
    public ApiResponse<JobPositionResponse> create(@Valid @RequestBody JobPositionRequest request) {
        return ApiResponse.ok(jobPositionService.create(request));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/qlns/job-position/{id}", action = "UPDATE")
    public ApiResponse<JobPositionResponse> update(@PathVariable String id,
                                                   @Valid @RequestBody JobPositionRequest request) {
        return ApiResponse.ok(jobPositionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qlns/job-position/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        jobPositionService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/category-usage")
    @Operation(summary = "Kiểm tra hạng mục đang được vị trí công việc sử dụng")
    @CheckPermission(api = "/qlns/job-position/category-usage", action = "VIEW")
    public ApiResponse<CategoryUsageResponse> categoryUsage(@RequestParam String categoryCode) {
        return ApiResponse.ok(jobPositionService.checkCategoryUsage(categoryCode));
    }
}
