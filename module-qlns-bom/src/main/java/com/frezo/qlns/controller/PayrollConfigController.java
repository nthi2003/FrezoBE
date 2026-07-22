package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.InsuranceConfigRequest;
import com.frezo.qlns.dto.request.PayrollConfigRequest;
import com.frezo.qlns.dto.request.TaxConfigRequest;
import com.frezo.qlns.dto.response.InsuranceConfigResponse;
import com.frezo.qlns.dto.response.PayrollConfigResponse;
import com.frezo.qlns.dto.response.TaxConfigResponse;
import com.frezo.qlns.service.InsuranceConfigService;
import com.frezo.qlns.service.PayrollConfigService;
import com.frezo.qlns.service.TaxConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/payroll-config")
@RequiredArgsConstructor
@Tag(name = "Cấu hình bảng lương", description = "API cấu hình lương, bảo hiểm, thuế")
public class PayrollConfigController {

    private final PayrollConfigService payrollConfigService;
    private final InsuranceConfigService insuranceConfigService;
    private final TaxConfigService taxConfigService;

    @Operation(summary = "Tạo/cập nhật cấu hình lương")
    @PostMapping("/payroll")
    public ApiResponse<PayrollConfigResponse> savePayrollConfig(@RequestBody PayrollConfigRequest request) {
        return ApiResponse.success(payrollConfigService.save(request));
    }

    @Operation(summary = "Cấu hình lương theo tổ chức và năm")
    @GetMapping("/payroll")
    public ApiResponse<PayrollConfigResponse> getPayrollConfig(
            @RequestParam String orgId, @RequestParam Integer year) {
        var result = payrollConfigService.getByOrgAndYear(orgId, year);
        if (result == null) {
            return ApiResponse.error("Không tìm thấy cấu hình lương");
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "Tạo/cập nhật cấu hình bảo hiểm")
    @PostMapping("/insurance")
    public ApiResponse<InsuranceConfigResponse> saveInsuranceConfig(@RequestBody InsuranceConfigRequest request) {
        return ApiResponse.success(insuranceConfigService.save(request));
    }

    @Operation(summary = "Cấu hình bảo hiểm theo năm")
    @GetMapping("/insurance")
    public ApiResponse<InsuranceConfigResponse> getInsuranceConfig(@RequestParam Integer year) {
        return ApiResponse.success(insuranceConfigService.getByYear(year));
    }

    @Operation(summary = "Tạo bậc thuế TNCN mới")
    @PostMapping("/tax")
    public ApiResponse<TaxConfigResponse> saveTaxConfig(@RequestBody TaxConfigRequest request) {
        return ApiResponse.success(taxConfigService.save(request));
    }

    @Operation(summary = "Danh sách bậc thuế theo năm")
    @GetMapping("/tax")
    public ApiResponse<List<TaxConfigResponse>> getTaxConfigs(@RequestParam Integer year) {
        return ApiResponse.success(taxConfigService.getByYear(year));
    }
}
