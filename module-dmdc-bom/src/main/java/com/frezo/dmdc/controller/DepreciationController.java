package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtbv.dto.request.DepreciationScheduleRequest;
import com.frezo.qtbv.dto.response.DepreciationPostingResponse;
import com.frezo.qtbv.dto.response.DepreciationScheduleResponse;
import com.frezo.qtbv.service.DepreciationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Khấu hao TSCĐ & post GL định kỳ.
 * <p>Endpoint mount song song {@code /qtbv/depreciation} và {@code /asset/depreciation}
 * để backward-compat với FE (giống pattern AssetController).
 * <p>Permission key canonical: {@code /asset/depreciation/...} (khớp seed STAFF deny / role filters).
 */
@RestController
@RequestMapping({"/qtbv/depreciation", "/asset/depreciation"})
@RequiredArgsConstructor
@Tag(name = "Depreciation - TSCĐ", description = "Lịch khấu hao + post GL")
public class DepreciationController {

    private final DepreciationService depreciationService;

    @Operation(summary = "Sinh lịch khấu hao cho 1 tài sản (trùng → SCHEDULE_EXISTS)")
    @PostMapping("/schedules/generate")
    @CheckPermission(api = "/asset/depreciation/schedules/generate", action = "CREATE")
    public ApiResponse<DepreciationScheduleResponse> generate(@RequestParam String assetId,
                                                              @RequestParam(required = false) String method,
                                                              @RequestParam(required = false) Integer months) {
        DepreciationScheduleRequest req = new DepreciationScheduleRequest();
        req.setAssetId(assetId);
        req.setMethod(method);
        req.setMonths(months);
        return ApiResponse.ok(depreciationService.generateSchedule(req));
    }

    @Operation(summary = "Danh sách schedule (lọc theo assetId nếu có)")
    @GetMapping("/schedules")
    @CheckPermission(api = "/asset/depreciation/schedules", action = "VIEW")
    public ApiResponse<List<DepreciationScheduleResponse>> list(
            @RequestParam(required = false) String assetId) {
        return ApiResponse.ok(depreciationService.listSchedules(assetId));
    }

    @Operation(summary = "Ghi sổ khấu hao 1 kỳ (idempotent DEP-YYYY-MM; PERIOD_CLOSED nếu kỳ đóng)")
    @PostMapping("/post")
    @CheckPermission(api = "/asset/depreciation/post", action = "UPDATE")
    public ApiResponse<DepreciationPostingResponse> post(@RequestParam int year,
                                                         @RequestParam int month) {
        return ApiResponse.ok(depreciationService.postPeriod(year, month));
    }

    @Operation(summary = "Preview khấu hao kỳ (không ghi GL)")
    @GetMapping("/preview")
    @CheckPermission(api = "/asset/depreciation/preview", action = "VIEW")
    public ApiResponse<DepreciationPostingResponse> preview(@RequestParam int year,
                                                            @RequestParam int month) {
        return ApiResponse.ok(depreciationService.previewPeriod(year, month));
    }

    @Operation(summary = "Danh sách posting theo năm/tháng")
    @GetMapping("/postings")
    @CheckPermission(api = "/asset/depreciation/postings", action = "VIEW")
    public ApiResponse<List<DepreciationPostingResponse>> postings(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.ok(depreciationService.listPostings(year, month));
    }
}
