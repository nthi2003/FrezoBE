package com.frezo.profit.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.profit.dto.request.ProfitCalculateRequest;
import com.frezo.profit.service.ProfitCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profit")
@RequiredArgsConstructor
@Tag(name = "Profit — Calculator", description = "Mô phỏng doanh thu & lợi nhuận ròng")
public class ProfitCalculatorController {

    private final ProfitCalculatorService profitCalculatorService;

    @PostMapping("/calculate")
    @CheckPermission(api = "/profit/calculate", action = "VIEW")
    @Operation(summary = "Tính lợi nhuận ròng", description = "Doanh thu − COGS − chi phí biến đổi − phân bổ chi phí cố định")
    public ApiResponse<?> calculate(@Valid @RequestBody ProfitCalculateRequest req) {
        return ApiResponse.success(profitCalculatorService.calculate(req));
    }
}
