package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.dto.request.SystemJobUpdateRequest;
import com.frezo.qtht.dto.response.SystemJobDto;
import com.frezo.qtht.dto.response.SystemJobHistoryDto;
import com.frezo.qtht.service.SystemJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/qtht/jobs")
@RequiredArgsConstructor
@Tag(name = "9. Quản trị hệ thống", description = "Tác vụ nền: cron, bật tắt, chạy tay, lịch sử")
public class SystemJobController {

    private final SystemJobService systemJobService;

    @GetMapping
    @CheckPermission(api = "/qtht/jobs", action = "VIEW")
    @Operation(summary = "Danh sách tác vụ nền")
    public ApiResponse<List<SystemJobDto>> list() {
        return ApiResponse.ok(systemJobService.listJobs());
    }

    @PutMapping("/{code}")
    @CheckPermission(api = "/qtht/jobs/{code}", action = "UPDATE")
    @Operation(summary = "Cập nhật cron / bật tắt tác vụ")
    public ApiResponse<SystemJobDto> update(@PathVariable String code,
                                            @RequestBody SystemJobUpdateRequest request) {
        return ApiResponse.ok(systemJobService.updateJob(code, request));
    }

    @PostMapping("/{code}/run")
    @CheckPermission(api = "/qtht/jobs/{code}/run", action = "EXECUTE")
    @Operation(summary = "Chạy tác vụ ngay", description = "Chạy bất đồng bộ — theo dõi kết quả ở tab lịch sử")
    public ApiResponse<SystemJobDto> run(@PathVariable String code) {
        return ApiResponse.ok(systemJobService.runNow(code));
    }

    @GetMapping("/{code}/history")
    @CheckPermission(api = "/qtht/jobs/{code}/history", action = "VIEW")
    @Operation(summary = "Lịch sử chạy của tác vụ")
    public ApiResponse<FePage<SystemJobHistoryDto>> history(
            @PathVariable String code,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        return ApiResponse.ok(systemJobService.history(code, pageNumber, pageSize, status, fromDate, toDate));
    }

    @GetMapping("/preview-cron")
    @CheckPermission(api = "/qtht/jobs", action = "VIEW")
    @Operation(summary = "Xem trước các mốc chạy kế tiếp của biểu thức cron")
    public ApiResponse<List<String>> previewCron(@RequestParam String expression,
                                                 @RequestParam(defaultValue = "5") Integer count) {
        return ApiResponse.ok(systemJobService.previewCron(expression, count));
    }
}
