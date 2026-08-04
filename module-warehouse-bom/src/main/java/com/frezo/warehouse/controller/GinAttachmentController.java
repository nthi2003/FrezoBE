package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.entity.GinAttachment;
import com.frezo.warehouse.service.GinAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/warehouse/gin/{ginId}/attachments")
@RequiredArgsConstructor
@Tag(name = "31. Xuất kho", description = "API quản lý file đính kèm phiếu xuất")
public class GinAttachmentController {

    private final GinAttachmentService ginAttachmentService;

    @Operation(summary = "Tải file lên", description = "Upload file đính kèm vào phiếu xuất kho")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckPermission(api = "/warehouse/gin/{ginId}/attachments", action = "UPDATE")
    public ApiResponse<GinAttachment> upload(
            @PathVariable String ginId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String note) {
        return ApiResponse.success(ginAttachmentService.upload(ginId, file, note));
    }

    @Operation(summary = "Danh sách file đính kèm")
    @GetMapping
    @CheckPermission(api = "/warehouse/gin/{ginId}/attachments", action = "VIEW")
    public ApiResponse<List<GinAttachment>> list(@PathVariable String ginId) {
        return ApiResponse.success(ginAttachmentService.listByGinId(ginId));
    }

    @Operation(summary = "Xoá file đính kèm")
    @DeleteMapping("/{attachmentId}")
    @CheckPermission(api = "/warehouse/gin/{ginId}/attachments/{attachmentId}", action = "DELETE")
    public ApiResponse<String> delete(@PathVariable String attachmentId) {
        ginAttachmentService.delete(attachmentId);
        return ApiResponse.success("Xoá file đính kèm thành công");
    }
}
