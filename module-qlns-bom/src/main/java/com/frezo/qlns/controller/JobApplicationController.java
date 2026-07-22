package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.JobApplicationRequest;
import com.frezo.qlns.dto.response.JobApplicationResponse;
import com.frezo.qlns.service.JobApplicationService;
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
@RequestMapping("/qlns/recruitment/applications")
@RequiredArgsConstructor
@Tag(name = "Recruitment - Application", description = "Đơn ứng tuyển & workflow stage")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @Operation(summary = "Ứng viên nộp đơn ứng tuyển vào 1 requisition")
    @PostMapping
    public ApiResponse<JobApplicationResponse> create(@RequestBody JobApplicationRequest req) {
        return ApiResponse.ok(jobApplicationService.create(req));
    }

    @Operation(summary = "Danh sách đơn ứng tuyển (lọc theo requisition & stage)")
    @GetMapping
    public ApiResponse<List<JobApplicationResponse>> list(
            @RequestParam(required = false) String requisitionId,
            @RequestParam(required = false) String stage) {
        return ApiResponse.ok(jobApplicationService.list(requisitionId, stage));
    }

    @Operation(summary = "Chi tiết đơn ứng tuyển")
    @GetMapping("/{id}")
    public ApiResponse<JobApplicationResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(jobApplicationService.getById(id));
    }

    @Operation(summary = "Chuyển stage của đơn (server tự validate transition hợp lệ)")
    @PostMapping("/{id}/move")
    public ApiResponse<JobApplicationResponse> move(@PathVariable String id,
                                                    @RequestParam String stage) {
        return ApiResponse.ok(jobApplicationService.moveStage(id, stage));
    }

    @Operation(summary = "Từ chối đơn ứng tuyển")
    @PostMapping("/{id}/reject")
    public ApiResponse<JobApplicationResponse> reject(@PathVariable String id,
                                                      @RequestParam(required = false) String reason) {
        return ApiResponse.ok(jobApplicationService.reject(id, reason));
    }

    @Operation(summary = "Hire — chuyển thẳng HIRED (từ OFFER hoặc nội bộ)")
    @PostMapping("/{id}/hire")
    public ApiResponse<JobApplicationResponse> hire(@PathVariable String id) {
        return ApiResponse.ok(jobApplicationService.markHired(id));
    }
}
