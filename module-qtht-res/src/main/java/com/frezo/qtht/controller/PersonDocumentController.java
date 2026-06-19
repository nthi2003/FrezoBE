package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.entity.PersonDocument;
import com.frezo.qtht.service.PersonDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/qtht/person-document")
@RequiredArgsConstructor
@Tag(name = "Person Document", description = "Quản lý tài liệu nhân sự (CV, chứng chỉ, thành tích)")
public class PersonDocumentController {

    private final PersonDocumentService personDocumentService;

    @Operation(summary = "Lấy danh sách tài liệu", description = "Lấy tài liệu của nhân viên theo personId, có thể lọc theo type (CV, CERTIFICATE, ACHIEVEMENT)")
    @GetMapping("/{personId}")
    public ResponseEntity<ApiResponse<List<PersonDocument>>> getDocuments(
            @PathVariable String personId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.success(personDocumentService.getDocuments(personId, type)));
    }

    @Operation(summary = "Upload tài liệu", description = "Upload CV/chứng chỉ/thành tích cho nhân viên")
    @PostMapping(value = "/{personId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PersonDocument>> uploadDocument(
            @PathVariable String personId,
            @RequestParam String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                personDocumentService.uploadDocument(personId, type, title, description, file)));
    }

    @Operation(summary = "Xoá tài liệu", description = "Xoá một tài liệu của nhân viên")
    @DeleteMapping("/{personId}/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String personId,
            @PathVariable String documentId) {
        personDocumentService.deleteDocument(personId, documentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
