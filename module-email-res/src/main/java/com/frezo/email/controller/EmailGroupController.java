package com.frezo.email.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.email.dto.request.EmailGroupRequest;
import com.frezo.email.dto.response.EmailGroupResponse;
import com.frezo.email.service.EmailGroupService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/email/group")
@RequiredArgsConstructor
@Tag(name = "Email Group Management", description = "APIs for managing email groups (mailing lists)")
public class EmailGroupController {

    private final EmailGroupService emailGroupService;

    @Operation(summary = "Get all email groups")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailGroupResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(emailGroupService.getAll()));
    }

    @Operation(summary = "Get email group by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailGroupResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(emailGroupService.getById(id)));
    }

    @Operation(summary = "Create new email group")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody EmailGroupRequest request) {
        Response<EmailGroupResponse> response = emailGroupService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }

    @Operation(summary = "Update email group")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id, @Valid @RequestBody EmailGroupRequest request) {
        Response<EmailGroupResponse> response = emailGroupService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }

    @Operation(summary = "Delete email group")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        emailGroupService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
