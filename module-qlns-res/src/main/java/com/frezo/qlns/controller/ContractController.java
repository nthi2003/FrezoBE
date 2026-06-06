package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.ContractAddRequest;
import com.frezo.qlns.dto.request.ContractAssginWorkAddRequest;
import com.frezo.qlns.dto.request.ContractEditRequest;
import com.frezo.qlns.dto.request.ContractFilter;
import com.frezo.qlns.dto.response.*;
import com.frezo.qlns.service.ContractService;
import com.frezo.qlns.service.ContractVersionService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/contract")
@RequiredArgsConstructor
@Tag(name = "Quản lí hợp đồng", description = "API quản lí hợp đồng")
public class ContractController {
    private final ContractService contractService;
    private final ContractVersionService contractVersionService;


    @Operation(summary = "Tạo hợp đồng mới" , description = "Tạo hợp đồng mới")
    @PostMapping
    public ApiResponse<?> createContract(@RequestBody ContractAddRequest request) {
        return ApiResponse.success(contractService.add(request));
    }

    @Operation(summary = "Cập nhật hợp đồng mới" , description = "Cập nhật hợp đồng ")
    @PutMapping("/{id}")
    public ApiResponse<?> updateContract(@PathVariable("id") String id , @RequestBody ContractEditRequest request) {
        return ApiResponse.success(contractService.edit(id , request));
    }

    @Operation(summary = "Xóa hợp đồng", description = "Xóa hợp đồng")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable("id") String id) {
        return ApiResponse.success(contractService.delete(id));
    }

    @Operation(summary = "Lấy danh sách hợp đồng" , description = "Lấy danh sách hợp đồng")
    @GetMapping
    public ApiResponse<?> all (@ModelAttribute ContractFilter filter) {
          return ApiResponse.success(contractService.all(filter));
    }

    @Operation(summary = "Combobox hợp đồng" , description = "Combobox hợp đồng")
    @GetMapping("/combobox")
    public ApiResponse<Response<List<ContractComboboxResponse>>> combobox (@ModelAttribute ContractFilter filter) {
        return ApiResponse.success(contractService.combobox(filter));
    }

    @Operation(summary = "Xem chi tiết hợp đồng" , description = "Xem chi tiết hợp đồng")
    @GetMapping("/{id}")
    public ApiResponse<ContractResponse> view (@PathVariable("id") String id) {
        return ApiResponse.success(contractService.view(id));
    }

    @Operation(summary = "Giao việc" , description = "Giao việc")
    @PostMapping("/{contractId}/assign")
    public ApiResponse<ContractAsginWorkResponse> assginWork (@PathVariable("contractId") String contractId , @RequestBody ContractAssginWorkAddRequest request) {
        return ApiResponse.success(contractService.assginWork(contractId, request));
    }

    @Operation(summary = "Lấy thông tin giao việc" , description = "Lấy thông tin giao việc của hợp đồng")
    @GetMapping("/{contractId}/assign")
    public ApiResponse<ContractAsginWorkResponse> getAssignWork (@PathVariable("contractId") String contractId) {
        return ApiResponse.success(contractService.getAssignWork(contractId));
    }

    @Operation(summary = "Cập nhât trạng thái hồ sơ" , description = "Cập nhât trạng thái hồ sơ")
    @PutMapping("/{id}/update-status")
    public ApiResponse<ContractResponse> updateStatus (@PathVariable("id") String id , @RequestBody ContractAddRequest request) {
        return ApiResponse.success(contractService.updateStatus(id, request));
    }

    @Operation(summary = "Từ chối hợp đồng", description = "Từ chối hợp đồng")
    @PutMapping("/{id}/reject")
    public ApiResponse<ContractResponse> reject (@PathVariable("id") String id) {
        return ApiResponse.success(contractService.reject(id));
    }

    // ─── Version History APIs ────────────────────────────────────────────────────

    @Operation(summary = "Lấy lịch sử phiên bản hợp đồng",
               description = "Danh sách tất cả versions của hợp đồng, sắp xếp mới nhất lên đầu")
    @GetMapping("/{contractId}/versions")
    public ApiResponse<List<ContractVersionListResponse>> getVersions (@PathVariable("contractId") String contractId) {
        return ApiResponse.success(contractVersionService.getVersionsByContractId(contractId));
    }

    @Operation(summary = "So sánh 2 phiên bản hợp đồng",
               description = "Trả về danh sách các field đã thay đổi giữa 2 version để highlight trên UI")
    @GetMapping("/{contractId}/versions/diff")
    public ApiResponse<ContractDiffResponse> diffVersions (
            @PathVariable("contractId") String contractId,
            @RequestParam("from") Integer fromVersion,
            @RequestParam("to") Integer toVersion) {
        return ApiResponse.success(contractVersionService.diffVersions(contractId, fromVersion, toVersion));
    }

}
