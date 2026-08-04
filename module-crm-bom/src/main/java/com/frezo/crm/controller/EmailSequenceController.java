package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.crm.dto.EmailSequenceEnrollRequest;
import com.frezo.crm.dto.EmailSequenceEnrollmentResponse;
import com.frezo.crm.dto.EmailSequenceRequest;
import com.frezo.crm.dto.EmailSequenceResponse;
import com.frezo.crm.service.EmailSequenceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/crm/email-sequences")
@RequiredArgsConstructor
@Tag(name = "CRM — Email Sequences")
public class EmailSequenceController {

    private final EmailSequenceService emailSequenceService;

    @GetMapping
    @CheckPermission(api = "/crm/email-sequences", action = "VIEW")
    public ApiResponse<List<EmailSequenceResponse>> list() {
        return ApiResponse.ok(emailSequenceService.list());
    }

    @PostMapping
    @CheckPermission(api = "/crm/email-sequences", action = "CREATE")
    public ApiResponse<EmailSequenceResponse> create(@RequestBody EmailSequenceRequest req) {
        return ApiResponse.ok(emailSequenceService.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/crm/email-sequences/{id}", action = "UPDATE")
    public ApiResponse<EmailSequenceResponse> update(
            @PathVariable String id, @RequestBody EmailSequenceRequest req) {
        return ApiResponse.ok(emailSequenceService.update(id, req));
    }

    @PostMapping("/{id}/enroll")
    @CheckPermission(api = "/crm/email-sequences/{id}/enroll", action = "CREATE")
    public ApiResponse<EmailSequenceEnrollmentResponse> enroll(
            @PathVariable String id, @RequestBody EmailSequenceEnrollRequest req) {
        return ApiResponse.ok(emailSequenceService.enroll(id, req));
    }
}
