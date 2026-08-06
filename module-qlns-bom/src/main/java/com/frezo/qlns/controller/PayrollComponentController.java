package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.PayrollComponentRequest;
import com.frezo.qlns.dto.response.PayrollComponentResponse;
import com.frezo.qlns.service.PayrollComponentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/payroll-component")
@RequiredArgsConstructor
@Tag(name = "HR - Phụ cấp / Khấu trừ")
public class PayrollComponentController {

    private final PayrollComponentService payrollComponentService;

    @GetMapping
    @CheckPermission(api = "/qlns/payroll-component", action = "VIEW")
    public ApiResponse<List<PayrollComponentResponse>> list() {
        return ApiResponse.ok(payrollComponentService.list());
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/payroll-component/{id}", action = "VIEW")
    public ApiResponse<PayrollComponentResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(payrollComponentService.getById(id));
    }

    @PostMapping
    @CheckPermission(api = "/qlns/payroll-component", action = "CREATE")
    public ApiResponse<PayrollComponentResponse> create(@Valid @RequestBody PayrollComponentRequest request) {
        return ApiResponse.ok(payrollComponentService.create(request));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/qlns/payroll-component/{id}", action = "UPDATE")
    public ApiResponse<PayrollComponentResponse> update(@PathVariable String id,
                                                        @Valid @RequestBody PayrollComponentRequest request) {
        return ApiResponse.ok(payrollComponentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qlns/payroll-component/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        payrollComponentService.delete(id);
        return ApiResponse.ok();
    }
}
