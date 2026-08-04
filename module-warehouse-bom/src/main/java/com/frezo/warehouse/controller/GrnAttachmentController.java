package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.entity.GrnAttachment;
import com.frezo.warehouse.service.GrnAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/warehouse/grn/{grnId}/attachments")
@RequiredArgsConstructor
@Tag(name = "30. Nhập kho", description = "API quản lý file đính kèm phiếu nhập")
public class GrnAttachmentController {

    private final GrnAttachmentService grnAttachmentService;

    @Operation(summary = "Tải file lên", description = "Upload file đính kèm vào phiếu nhập kho")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckPermission(api = "/warehouse/grn/{grnId}/attachments", action = "UPDATE")
    public ApiResponse<GrnAttachment> upload(
            @PathVariable String grnId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String note) {
        return ApiResponse.success(grnAttachmentService.upload(grnId, file, note));
    }

    @Operation(summary = "Danh sách file đính kèm")
    @GetMapping
    @CheckPermission(api = "/warehouse/grn/{grnId}/attachments", action = "VIEW")
    public ApiResponse<List<GrnAttachment>> list(@PathVariable String grnId) {
        return ApiResponse.success(grnAttachmentService.listByGrnId(grnId));
    }

    @Operation(summary = "Xoá file đính kèm")
    @DeleteMapping("/{attachmentId}")
    @CheckPermission(api = "/warehouse/grn/{grnId}/attachments/{attachmentId}", action = "DELETE")
    public ApiResponse<String> delete(@PathVariable String attachmentId) {
        grnAttachmentService.delete(attachmentId);
        return ApiResponse.success("Xoá file đính kèm thành công");
    }
}
