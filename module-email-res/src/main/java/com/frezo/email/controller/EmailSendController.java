package com.frezo.email.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.email.dto.request.BulkEmailRequest;
import com.frezo.email.dto.response.BulkEmailResponse;
import com.frezo.email.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/email/send")
@RequiredArgsConstructor
@Tag(name = "Email Send", description = "APIs for sending emails")
public class EmailSendController {

    private final EmailService emailService;

    @Operation(summary = "Gửi email đồng loạt", description = "Gửi email đến danh sách người nhận hoặc theo nhóm khách hàng")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkEmailResponse>> sendBulk(@Valid @RequestBody BulkEmailRequest request) {
        BulkEmailResponse response = emailService.sendBulk(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Gửi email theo nhóm khách hàng", description = "Gửi email đến tất cả khách hàng trong nhóm category")
    @PostMapping("/by-group")
    public ResponseEntity<ApiResponse<BulkEmailResponse>> sendByGroup(
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String body,
            @RequestParam List<String> categoryCodes,
            @RequestParam(required = false) String description) {
        BulkEmailResponse response = emailService.sendBulkByCategoryCodes(
                templateCode, subject, body, categoryCodes, description);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
