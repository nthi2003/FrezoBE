package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.MinioService;
import com.frezo.qlns.dto.response.ContractTemplateResponse;
import com.frezo.qlns.service.ContractTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/qlns/contract-template")
@RequiredArgsConstructor
@Tag(name = "Quản lí mẫu hợp đồng", description = "API quản lí mẫu hợp đồng (lưu file HTML trên MinIO)")
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;
    private final MinioService minioService;

    @Operation(summary = "Lấy danh sách mẫu hợp đồng", description = "Trả về danh sách mẫu đã lưu (chưa bị xoá), sắp xếp mới nhất lên đầu")
    @GetMapping
    public ApiResponse<List<ContractTemplateResponse>> getAll() {
        return ApiResponse.success(contractTemplateService.getAll());
    }

    @Operation(summary = "Tạo mẫu hợp đồng mới",
               description = "Upload file HTML nội dung mẫu lên MinIO, lưu metadata vào database")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ContractTemplateResponse> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("type") String type) {
        String originalName = file.getOriginalFilename();
        String objectName = "contract-templates/" + UUID.randomUUID() + "_" + (originalName != null ? originalName : "template.html");
        String fileUrl = minioService.uploadFile(objectName, file, "frezo-contact");
        return ApiResponse.success(contractTemplateService.create(name, type, fileUrl, objectName));
    }

    @Operation(summary = "Xoá mẫu hợp đồng",
               description = "Xoá mềm metadata và xoá file trên MinIO")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") String id) {
        contractTemplateService.delete(id);
        return ApiResponse.success(null);
    }
}
