package com.frezo.common.controller;

import com.frezo.common.model.ai.AiOcrAnalyzeResponse;
import com.frezo.common.model.ai.AiOcrReviewSaveRequest;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.AiDocumentService;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai/doc")
public class AiDocController {

    private final AiDocumentService aiDocumentService;

    public AiDocController(AiDocumentService aiDocumentService) {
        this.aiDocumentService = aiDocumentService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AiOcrAnalyzeResponse>> analyzeDocument(
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        AiOcrAnalyzeResponse response = aiDocumentService.analyzeDocument(file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/review")
    public ResponseEntity<ApiResponse<AiOcrAnalyzeResponse>> reviewDocument(
        @RequestBody AiOcrReviewSaveRequest request
    ) {
        AiOcrAnalyzeResponse response = aiDocumentService.saveReviewedDocument(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}