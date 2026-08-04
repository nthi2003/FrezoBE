package com.frezo.fbautomation.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.fbautomation.dto.request.CommentRuleRequest;
import com.frezo.fbautomation.dto.request.ModeratedCommentRequest;
import com.frezo.fbautomation.dto.response.CommentRuleResponse;
import com.frezo.fbautomation.dto.response.ModeratedCommentResponse;
import com.frezo.fbautomation.service.CommentModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mkt/comments")
@RequiredArgsConstructor
@Tag(name = "MKT · Comments", description = "Kiểm duyệt comment + rule từ khoá (MVP offline)")
public class CommentModerationController {

    private final CommentModerationService service;

    @GetMapping("/dashboard")
    @CheckPermission(api = "/mkt/comments/dashboard", action = "VIEW")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }

    // ---- Rules (literal paths before {id}) ----
    @GetMapping("/rules")
    @CheckPermission(api = "/mkt/comments/rules", action = "VIEW")
    public ApiResponse<List<CommentRuleResponse>> listRules() {
        return ApiResponse.ok(service.listRules());
    }

    @PostMapping("/rules")
    @CheckPermission(api = "/mkt/comments/rules", action = "CREATE")
    public ApiResponse<CommentRuleResponse> createRule(@RequestBody @Valid CommentRuleRequest req) {
        return ApiResponse.ok(service.createRule(req));
    }

    @GetMapping("/rules/{ruleId}")
    @CheckPermission(api = "/mkt/comments/rules/{ruleId}", action = "VIEW")
    public ApiResponse<CommentRuleResponse> getRule(@PathVariable String ruleId) {
        return ApiResponse.ok(service.getRule(ruleId));
    }

    @PutMapping("/rules/{ruleId}")
    @CheckPermission(api = "/mkt/comments/rules/{ruleId}", action = "UPDATE")
    public ApiResponse<CommentRuleResponse> updateRule(
            @PathVariable String ruleId,
            @RequestBody @Valid CommentRuleRequest req) {
        return ApiResponse.ok(service.updateRule(ruleId, req));
    }

    @DeleteMapping("/rules/{ruleId}")
    @CheckPermission(api = "/mkt/comments/rules/{ruleId}", action = "DELETE")
    public ApiResponse<Void> deleteRule(@PathVariable String ruleId) {
        service.deleteRule(ruleId);
        return ApiResponse.ok();
    }

    // ---- Comment queue ----
    @GetMapping
    @CheckPermission(api = "/mkt/comments", action = "VIEW")
    public ApiResponse<List<ModeratedCommentResponse>> list(
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.listComments(status));
    }

    @PostMapping
    @CheckPermission(api = "/mkt/comments", action = "CREATE")
    public ApiResponse<ModeratedCommentResponse> create(@RequestBody @Valid ModeratedCommentRequest req) {
        return ApiResponse.ok(service.createComment(req));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/mkt/comments/{id}", action = "VIEW")
    public ApiResponse<ModeratedCommentResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.getComment(id));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/mkt/comments/{id}", action = "UPDATE")
    public ApiResponse<ModeratedCommentResponse> update(
            @PathVariable String id,
            @RequestBody @Valid ModeratedCommentRequest req) {
        return ApiResponse.ok(service.updateComment(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/mkt/comments/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.deleteComment(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/moderate")
    @CheckPermission(api = "/mkt/comments/{id}/moderate", action = "UPDATE")
    @Operation(summary = "Áp dụng hành động kiểm duyệt: HIDE|REPLY|FLAG|IGNORE")
    public ApiResponse<ModeratedCommentResponse> moderate(
            @PathVariable String id,
            @RequestParam String action,
            @RequestParam(required = false) String replyText) {
        return ApiResponse.ok(service.moderate(id, action, replyText));
    }
}
