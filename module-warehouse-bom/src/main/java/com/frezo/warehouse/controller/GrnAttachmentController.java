package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.MinioService;
import com.frezo.warehouse.entity.GrnAttachment;
import com.frezo.warehouse.repository.GrnAttachmentRepository;
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

    private final GrnAttachmentRepository attachmentRepository;
    private final MinioService minioService;

    @Operation(summary = "Tải file lên", description = "Upload file đính kèm vào phiếu nhập kho")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(
            @PathVariable String grnId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String note) {
        String objectName = "grn/" + grnId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = minioService.uploadFile(objectName, file);

        GrnAttachment attachment = GrnAttachment.builder()
                .grnId(grnId)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .note(note)
                .build();
        attachmentRepository.save(attachment);
        return ApiResponse.success(attachment);
    }

    @Operation(summary = "Danh sách file đính kèm")
    @GetMapping
    public ApiResponse<?> list(@PathVariable String grnId) {
        List<GrnAttachment> attachments = attachmentRepository.findByGrnId(grnId);
        return ApiResponse.success(attachments);
    }

    @Operation(summary = "Xoá file đính kèm")
    @DeleteMapping("/{attachmentId}")
    public ApiResponse<?> delete(@PathVariable String attachmentId) {
        attachmentRepository.deleteById(attachmentId);
        return ApiResponse.success("Xoá file đính kèm thành công");
    }
}
