package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.OnboardingAssignRequest;
import com.frezo.qlns.dto.request.OnboardingTemplateRequest;
import com.frezo.qlns.dto.response.OnboardingAssignmentResponse;
import com.frezo.qlns.dto.response.OnboardingTemplateResponse;
import com.frezo.qlns.service.OnboardingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/qlns/onboarding")
@RequiredArgsConstructor
@Tag(name = "QLNS — Onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/templates")
    @CheckPermission(api = "/qlns/onboarding/templates", action = "VIEW")
    public ApiResponse<List<OnboardingTemplateResponse>> listTemplates() {
        return ApiResponse.ok(onboardingService.listTemplates());
    }

    @PostMapping("/templates")
    @CheckPermission(api = "/qlns/onboarding/templates", action = "CREATE")
    public ApiResponse<OnboardingTemplateResponse> createTemplate(@RequestBody OnboardingTemplateRequest req) {
        return ApiResponse.ok(onboardingService.createTemplate(req));
    }

    @PutMapping("/templates/{id}")
    @CheckPermission(api = "/qlns/onboarding/templates/{id}", action = "UPDATE")
    public ApiResponse<OnboardingTemplateResponse> updateTemplate(
            @PathVariable String id, @RequestBody OnboardingTemplateRequest req) {
        return ApiResponse.ok(onboardingService.updateTemplate(id, req));
    }

    @GetMapping("/assignments")
    @CheckPermission(api = "/qlns/onboarding/assignments", action = "VIEW")
    public ApiResponse<List<OnboardingAssignmentResponse>> listAssignments(
            @RequestParam(required = false) String personId) {
        return ApiResponse.ok(onboardingService.listAssignments(personId));
    }

    @PostMapping("/assignments")
    @CheckPermission(api = "/qlns/onboarding/assignments", action = "UPDATE")
    public ApiResponse<OnboardingAssignmentResponse> assign(@RequestBody OnboardingAssignRequest req) {
        return ApiResponse.ok(onboardingService.assign(req));
    }

    @PostMapping("/assignments/{assignmentId}/items/{itemId}/complete")
    @CheckPermission(api = "/qlns/onboarding/assignments/{assignmentId}/items/{itemId}/complete", action = "UPDATE")
    public ApiResponse<OnboardingAssignmentResponse> completeItem(
            @PathVariable String assignmentId, @PathVariable String itemId) {
        return ApiResponse.ok(onboardingService.completeItem(assignmentId, itemId));
    }
}
