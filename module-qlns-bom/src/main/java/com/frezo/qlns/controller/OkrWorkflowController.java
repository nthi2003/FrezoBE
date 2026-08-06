package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.OkrWorkflowRequests;
import com.frezo.qlns.service.Impl.OkrWorkflowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qlns/okr-workflow")
@RequiredArgsConstructor
@Tag(name = "QLNS — OKR Workflow")
public class OkrWorkflowController {
    private final OkrWorkflowService service;

    @GetMapping("/cycles")
    @CheckPermission(api = "/qlns/okr-workflow/cycles", action = "VIEW")
    public ApiResponse<List<Map<String, Object>>> listCycles() {
        return ApiResponse.ok(service.listCycles());
    }

    @PostMapping("/cycles")
    @CheckPermission(api = "/qlns/okr-workflow/cycles", action = "CREATE")
    public ApiResponse<Map<String, Object>> createCycle(@RequestBody OkrWorkflowRequests.Cycle req) {
        return ApiResponse.ok(service.createCycle(req));
    }

    @PutMapping("/cycles/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/cycles/{id}", action = "UPDATE")
    public ApiResponse<Map<String, Object>> updateCycle(
            @PathVariable String id, @RequestBody OkrWorkflowRequests.Cycle req) {
        return ApiResponse.ok(service.updateCycle(id, req));
    }

    @DeleteMapping("/cycles/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/cycles/{id}", action = "DELETE")
    public ApiResponse<Void> deleteCycle(@PathVariable String id) {
        service.deleteCycle(id);
        return ApiResponse.ok();
    }

    @GetMapping("/timeline")
    @CheckPermission(api = "/qlns/okr-workflow/timeline", action = "VIEW")
    public ApiResponse<List<Map<String, Object>>> listTimeline() {
        return ApiResponse.ok(service.listTimeline());
    }

    @PostMapping("/timeline")
    @CheckPermission(api = "/qlns/okr-workflow/timeline", action = "CREATE")
    public ApiResponse<Map<String, Object>> createTimeline(@RequestBody OkrWorkflowRequests.TimelineStep req) {
        return ApiResponse.ok(service.createTimeline(req));
    }

    @PutMapping("/timeline/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/timeline/{id}", action = "UPDATE")
    public ApiResponse<Map<String, Object>> updateTimeline(
            @PathVariable String id, @RequestBody OkrWorkflowRequests.TimelineStep req) {
        return ApiResponse.ok(service.updateTimeline(id, req));
    }

    @DeleteMapping("/timeline/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/timeline/{id}", action = "DELETE")
    public ApiResponse<Void> deleteTimeline(@PathVariable String id) {
        service.deleteTimeline(id);
        return ApiResponse.ok();
    }

    @GetMapping("/feedback-types")
    @CheckPermission(api = "/qlns/okr-workflow/feedback-types", action = "VIEW")
    public ApiResponse<List<Map<String, Object>>> listFeedbackTypes() {
        return ApiResponse.ok(service.listFeedbackTypes());
    }

    @PostMapping("/feedback-types")
    @CheckPermission(api = "/qlns/okr-workflow/feedback-types", action = "CREATE")
    public ApiResponse<Map<String, Object>> createFeedbackType(@RequestBody OkrWorkflowRequests.FeedbackType req) {
        return ApiResponse.ok(service.createFeedbackType(req));
    }

    @PutMapping("/feedback-types/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/feedback-types/{id}", action = "UPDATE")
    public ApiResponse<Map<String, Object>> updateFeedbackType(
            @PathVariable String id, @RequestBody OkrWorkflowRequests.FeedbackType req) {
        return ApiResponse.ok(service.updateFeedbackType(id, req));
    }

    @DeleteMapping("/feedback-types/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/feedback-types/{id}", action = "DELETE")
    public ApiResponse<Void> deleteFeedbackType(@PathVariable String id) {
        service.deleteFeedbackType(id);
        return ApiResponse.ok();
    }

    @GetMapping("/feedback")
    @CheckPermission(api = "/qlns/okr-workflow/feedback", action = "VIEW")
    public ApiResponse<List<Map<String, Object>>> listFeedback() {
        return ApiResponse.ok(service.listFeedback());
    }

    @PostMapping("/feedback")
    @CheckPermission(api = "/qlns/okr-workflow/feedback", action = "CREATE")
    public ApiResponse<Map<String, Object>> createFeedback(@RequestBody OkrWorkflowRequests.Feedback req) {
        return ApiResponse.ok(service.createFeedback(req));
    }

    @GetMapping("/key-results/{keyResultId}/actions")
    @CheckPermission(api = "/qlns/okr-workflow/key-results/{keyResultId}/actions", action = "VIEW")
    public ApiResponse<List<Map<String, Object>>> listActions(@PathVariable String keyResultId) {
        return ApiResponse.ok(service.listActions(keyResultId));
    }

    @PostMapping("/key-results/{keyResultId}/actions")
    @CheckPermission(api = "/qlns/okr-workflow/key-results/{keyResultId}/actions", action = "CREATE")
    public ApiResponse<Map<String, Object>> createAction(
            @PathVariable String keyResultId, @RequestBody OkrWorkflowRequests.Action req) {
        return ApiResponse.ok(service.createAction(keyResultId, req));
    }

    @PutMapping("/actions/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/actions/{id}", action = "UPDATE")
    public ApiResponse<Map<String, Object>> updateAction(
            @PathVariable String id, @RequestBody OkrWorkflowRequests.Action req) {
        return ApiResponse.ok(service.updateAction(id, req));
    }

    @DeleteMapping("/actions/{id}")
    @CheckPermission(api = "/qlns/okr-workflow/actions/{id}", action = "DELETE")
    public ApiResponse<Void> deleteAction(@PathVariable String id) {
        service.deleteAction(id);
        return ApiResponse.ok();
    }

    @GetMapping("/okrs/{okrId}/check-ins")
    @CheckPermission(api = "/qlns/okr-workflow/okrs/{okrId}/check-ins", action = "VIEW")
    public ApiResponse<List<Map<String, Object>>> listCheckIns(@PathVariable String okrId) {
        return ApiResponse.ok(service.listCheckIns(okrId));
    }

    @PostMapping("/okrs/{okrId}/check-ins")
    @CheckPermission(api = "/qlns/okr-workflow/okrs/{okrId}/check-ins", action = "CREATE")
    public ApiResponse<Map<String, Object>> createCheckIn(
            @PathVariable String okrId, @RequestBody OkrWorkflowRequests.CheckIn req) {
        return ApiResponse.ok(service.createCheckIn(okrId, req));
    }

    @PostMapping("/check-ins/{id}/confirm")
    @CheckPermission(api = "/qlns/okr-workflow/check-ins/{id}/confirm", action = "UPDATE")
    public ApiResponse<Map<String, Object>> confirmCheckIn(
            @PathVariable String id, @RequestBody OkrWorkflowRequests.CheckIn req) {
        return ApiResponse.ok(service.confirmCheckIn(id, req));
    }

    @PostMapping("/check-ins/{id}/feedback")
    @CheckPermission(api = "/qlns/okr-workflow/check-ins/{id}/feedback", action = "CREATE")
    public ApiResponse<Map<String, Object>> addCheckInFeedback(
            @PathVariable String id, @RequestBody OkrWorkflowRequests.CheckInFeedback req) {
        return ApiResponse.ok(service.addCheckInFeedback(id, req));
    }
}
