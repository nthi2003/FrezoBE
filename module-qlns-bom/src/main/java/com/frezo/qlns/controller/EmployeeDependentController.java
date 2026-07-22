package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.EmployeeDependentRequest;
import com.frezo.qlns.dto.response.EmployeeDependentResponse;
import com.frezo.qlns.service.EmployeeDependentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/employee-dependent")
@RequiredArgsConstructor
@Tag(name = "Người phụ thuộc", description = "API quản lý người phụ thuộc cho giảm trừ thuế TNCN")
public class EmployeeDependentController {

    private final EmployeeDependentService employeeDependentService;

    @Operation(summary = "Thêm người phụ thuộc")
    @PostMapping
    public ApiResponse<EmployeeDependentResponse> create(@RequestBody EmployeeDependentRequest request) {
        return ApiResponse.success(employeeDependentService.create(request));
    }

    @Operation(summary = "Cập nhật người phụ thuộc")
    @PutMapping("/{id}")
    public ApiResponse<EmployeeDependentResponse> update(@PathVariable String id, @RequestBody EmployeeDependentRequest request) {
        return ApiResponse.success(employeeDependentService.update(id, request));
    }

    @Operation(summary = "Xóa người phụ thuộc")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        employeeDependentService.delete(id);
        return ApiResponse.success(null, "Xóa thành công");
    }

    @Operation(summary = "Danh sách người phụ thuộc của nhân viên")
    @GetMapping("/by-person/{personId}")
    public ApiResponse<List<EmployeeDependentResponse>> getByPersonId(@PathVariable String personId) {
        return ApiResponse.success(employeeDependentService.getByPersonId(personId));
    }
}
