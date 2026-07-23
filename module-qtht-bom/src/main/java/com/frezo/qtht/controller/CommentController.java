package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.qtht.dto.comment.CommentAttachmentDto;
import com.frezo.qtht.dto.comment.CommentCreatePayload;
import com.frezo.qtht.dto.comment.CommentDto;
import com.frezo.qtht.dto.comment.CommentUpdatePayload;
import com.frezo.qtht.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Thread", description = "Comment + @mention + đính kèm")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "List comments theo subject")
    public ApiResponse<FePage<CommentDto>> list(
            @RequestParam String subjectType,
            @RequestParam String subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(commentService.list(subjectType, subjectId, page, size));
    }

    @PostMapping
    @Operation(summary = "Tạo comment")
    public ApiResponse<CommentDto> create(@RequestBody CommentCreatePayload payload) {
        return ApiResponse.ok(commentService.create(payload));
    }

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file đính kèm comment (MinIO) — image/pdf/doc ≤10MB")
    public ApiResponse<CommentAttachmentDto> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(commentService.uploadAttachment(file));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa comment (author only)")
    public ApiResponse<CommentDto> update(@PathVariable String id,
                                          @RequestBody CommentUpdatePayload payload) {
        return ApiResponse.ok(commentService.update(id, payload));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete comment (author only)")
    public ApiResponse<Void> delete(@PathVariable String id) {
        commentService.delete(id);
        return ApiResponse.ok();
    }
}
