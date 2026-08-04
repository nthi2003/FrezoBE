package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.InterviewCompleteRequest;
import com.frezo.qlns.dto.request.InterviewRequest;
import com.frezo.qlns.dto.response.InterviewResponse;
import com.frezo.qlns.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/qlns/recruitment/interviews")
@RequiredArgsConstructor
@Tag(name = "Recruitment - Interview", description = "Đặt lịch phỏng vấn & ghi kết quả")
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Đặt buổi phỏng vấn")
    @PostMapping
    @CheckPermission(api = "/qlns/recruitment/interviews", action = "CREATE")
    public ApiResponse<InterviewResponse> create(@RequestBody InterviewRequest req) {
        return ApiResponse.ok(interviewService.create(req));
    }

    @Operation(summary = "Danh sách buổi phỏng vấn của 1 application")
    @GetMapping
    @CheckPermission(api = "/qlns/recruitment/interviews", action = "VIEW")
    public ApiResponse<List<InterviewResponse>> list(@RequestParam String applicationId) {
        return ApiResponse.ok(interviewService.list(applicationId));
    }

    @Operation(summary = "Hoàn tất phỏng vấn — kèm điểm & feedback")
    @PostMapping("/{id}/complete")
    @CheckPermission(api = "/qlns/recruitment/interviews/{id}/complete", action = "UPDATE")
    public ApiResponse<InterviewResponse> complete(@PathVariable String id,
                                                   @RequestBody(required = false) InterviewCompleteRequest req) {
        return ApiResponse.ok(interviewService.complete(id, req));
    }
}
