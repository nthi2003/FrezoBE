package com.frezo.email.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.email.dto.request.EmailConfigAddRequest;
import com.frezo.email.dto.request.EmailConfigEditRequest;
import com.frezo.email.dto.request.EmailConfigFilter;
import com.frezo.email.service.EmailConfigService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/email/config")
@RequiredArgsConstructor
@Tag(name = "Email Config Management", description = "APIs for managing email configurations")
public class EmailConfigController {
    private final EmailConfigService emailConfigService;

    @Operation(summary = "Get all email configurations", description = "Returns a paginated list of email configurations")
    @GetMapping("")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(EmailConfigFilter filter) {
        return ResponseEntity.ok(ApiResponse.success(emailConfigService.all(filter)));
    }

    @Operation(summary = "Add new email configuration")
    @PostMapping("")
    public ResponseEntity<ApiResponse<?>> add(@Valid @RequestBody EmailConfigAddRequest request) {
        Response<?> response = emailConfigService.add(request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }

    @Operation(summary = "Edit existing email configuration")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> edit(@PathVariable("id") String id,
            @Valid @RequestBody EmailConfigEditRequest request) {
        Response<?> response = emailConfigService.edit(id, request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }

    @Operation(summary = "Deactivate email configuration")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivate(@PathVariable("id") String id) {
        emailConfigService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Deactivated successfully"));
    }

    @Operation(summary = "Activate email configuration")
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activate(@PathVariable("id") String id) {
        emailConfigService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("Activated successfully"));
    }

    @Operation(summary = "Delete email Config")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        emailConfigService.delete(id);
    }

    @Operation(summary = "Test email configuration connection")
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<ApiResponse<String>> testConnection(@PathVariable("id") String id) {
        emailConfigService.testConnection(id);
        return ResponseEntity.ok(ApiResponse.success("Connection successful"));
    }
}
