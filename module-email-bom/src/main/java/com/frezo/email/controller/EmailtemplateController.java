package com.frezo.email.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.email.dto.request.EmailTemplateFilter;
import com.frezo.email.dto.request.EmailTemplateRequest;
import com.frezo.email.dto.request.SendTestEmailRequest;
import com.frezo.email.dto.response.EmailTemplateResponse;
import com.frezo.email.service.EmailService;
import com.frezo.email.service.EmailtemplateService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/email/template")
@RequiredArgsConstructor
@Tag(name = "Email Template Management", description = "APIs for managing email templates")
public class EmailtemplateController {

    private final EmailtemplateService emailTemplateService;
    private final EmailService emailService;

    @Operation(summary = "Get all email templates", description = "Returns a paginated list of email templates")
    @GetMapping("")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(EmailTemplateFilter filter) {
        return ResponseEntity.ok(ApiResponse.success(emailTemplateService.all(filter)));
    }

    @Operation(summary = "Add new email template")
    @PostMapping("")
    public ResponseEntity<ApiResponse<?>> add(@Valid @RequestBody EmailTemplateRequest request) {
        Response<?> response = emailTemplateService.add(request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }

    @Operation(summary = "Edit existing email template")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> edit(@PathVariable("id") String id,
            @Valid @RequestBody EmailTemplateRequest request) {
        Response<?> response = emailTemplateService.edit(id, request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }

    @Operation(summary = "View email template details")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> view(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(emailTemplateService.view(id)));
    }

    @Operation(summary = "Delete email template")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        emailTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Send test email using template")
    @PostMapping("/{id}/send-test")
    public ResponseEntity<ApiResponse<String>> sendTest(
            @PathVariable("id") String id,
            @Valid @RequestBody SendTestEmailRequest request) {
        EmailTemplateResponse template = emailTemplateService.view(id);
        emailService.sendByTemplate(template.getCode(), request.getParams(), request.getRecipients());
        return ResponseEntity.ok(ApiResponse.success("Email sent successfully"));
    }
}
