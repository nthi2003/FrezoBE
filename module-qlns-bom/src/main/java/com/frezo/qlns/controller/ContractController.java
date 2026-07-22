package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.MinioService;
import com.frezo.qlns.dto.request.*;
import com.frezo.qlns.dto.response.*;
import com.frezo.qlns.service.AiExtractionService;
import com.frezo.qlns.service.ContractService;
import com.frezo.qlns.service.ContractVersionService;
import com.frezo.qlns.service.DocumentExtractionService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/qlns/contract")
@RequiredArgsConstructor
@Tag(name = "Quản lí hợp đồng", description = "API quản lí hợp đồng")
public class ContractController {
    private final ContractService contractService;
    private final ContractVersionService contractVersionService;
    private final MinioService minioService;
    private final DocumentExtractionService documentExtractionService;
    private final AiExtractionService aiExtractionService;


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

    @Operation(summary = "Upload file hợp đồng", description = "Upload file .doc/.docx/.pdf lên MinIO bucket frezo-contact")
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String objectName = "contracts/" + UUID.randomUUID() + "_" + (originalName != null ? originalName : "document");
        String url = minioService.uploadFile(objectName, file, "frezo-contact");
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString());
        result.put("fileName", originalName);
        result.put("fileUrl", url);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Upload + trích xuất nội dung file hợp đồng",
               description = "Upload file .doc/.docx/.pdf, trích xuất nội dung và các trường dữ liệu tự động")
    @PostMapping("/upload-and-extract")
    public ApiResponse<Map<String, Object>> uploadAndExtract(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String objectName = "contracts/" + UUID.randomUUID() + "_" + (originalName != null ? originalName : "document");
        String url = minioService.uploadFile(objectName, file, "frezo-contact");
        Map<String, String> fields = documentExtractionService.extractFields(file);
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString());
        result.put("fileName", originalName);
        result.put("fileUrl", url);
        result.putAll(fields);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Lưu nội dung hợp đồng", description = "Lưu nội dung hợp đồng vào MinIO với tên là ID hợp đồng")
    @PostMapping("/{id}/save-content")
    public ApiResponse<Map<String, Object>> saveContent(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null) content = "";
        String objectName = "contracts/" + id + "/content.txt";
        String url = minioService.saveContent(objectName, content, "frezo-contact");
        Map<String, Object> result = new HashMap<>();
        result.put("contractId", id);
        result.put("contentUrl", url);
        return ApiResponse.success(result);
    }

    @Operation(summary = "AI chỉnh sửa nội dung", description = "Gửi nội dung hợp đồng sang AI để chỉnh sửa văn bản")
    @PostMapping("/{id}/ai-edit")
    public ApiResponse<Map<String, Object>> aiEdit(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> body
    ) {
        String text = body.getOrDefault("text", "");
        String instruction = body.getOrDefault("instruction", "chỉnh sửa văn bản cho chuyên nghiệp, sửa lỗi chính tả và ngữ pháp");
        contractService.updateAiStatus(id, "PROCESSING");
        String edited = aiExtractionService.editContent(text, instruction);
        boolean success = !edited.equals(text);
        contractService.updateAiStatus(id, success ? "SUCCESS" : "FAILED");
        if (success) {
            contractService.updateHtmlContract(id, edited);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("contractId", id);
        result.put("aiStatus", success ? "SUCCESS" : "FAILED");
        result.put("edited", edited);
        return ApiResponse.success(result);
    }

    @Operation(summary = "AI chỉnh sửa text thuần (trước khi tạo contract)",
               description = "Chỉnh sửa text trực tiếp qua AI, không cần contractId — dùng trên màn hình tạo hợp đồng")
    @PostMapping("/ai-edit")
    public ApiResponse<Map<String, Object>> aiEditText(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        String instruction = body.getOrDefault("instruction", "chỉnh sửa văn bản cho chuyên nghiệp, sửa lỗi chính tả và ngữ pháp");
        String edited = aiExtractionService.editContent(text, instruction);
        Map<String, Object> result = new HashMap<>();
        result.put("original", text);
        result.put("edited", edited);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Kiểm tra trạng thái AI", description = "Kiểm tra trạng thái xử lý AI của hợp đồng")
    @GetMapping("/{id}/ai-status")
    public ApiResponse<Map<String, Object>> checkAiStatus(@PathVariable("id") String id) {
        ContractResponse contract = contractService.view(id);
        Map<String, Object> result = new HashMap<>();
        result.put("contractId", id);
        result.put("aiStatus", contract.getAiStatus());
        return ApiResponse.success(result);
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
