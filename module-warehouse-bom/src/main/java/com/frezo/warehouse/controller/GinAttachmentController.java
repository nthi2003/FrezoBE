package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.MinioService;
import com.frezo.warehouse.entity.GinAttachment;
import com.frezo.warehouse.repository.GinAttachmentRepository;
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

    private final GinAttachmentRepository attachmentRepository;
    private final MinioService minioService;

    @Operation(summary = "Tải file lên", description = "Upload file đính kèm vào phiếu xuất kho")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(
            @PathVariable String ginId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String note) {
        String objectName = "gin/" + ginId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = minioService.uploadFile(objectName, file);

        GinAttachment attachment = GinAttachment.builder()
                .ginId(ginId)
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
    public ApiResponse<?> list(@PathVariable String ginId) {
        List<GinAttachment> attachments = attachmentRepository.findByGinId(ginId);
        return ApiResponse.success(attachments);
    }

    @Operation(summary = "Xoá file đính kèm")
    @DeleteMapping("/{attachmentId}")
    public ApiResponse<?> delete(@PathVariable String attachmentId) {
        attachmentRepository.deleteById(attachmentId);
        return ApiResponse.success("Xoá file đính kèm thành công");
    }
}
