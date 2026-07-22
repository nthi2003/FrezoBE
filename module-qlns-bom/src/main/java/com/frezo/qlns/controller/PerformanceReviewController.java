package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.ManagerScoreRequest;
import com.frezo.qlns.dto.request.PerformanceReviewRequest;
import com.frezo.qlns.dto.response.PerformanceReviewResponse;
import com.frezo.qlns.service.PerformanceReviewService;
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
@RequestMapping("/qlns/performance-reviews")
@RequiredArgsConstructor
@Tag(name = "QLNS — Performance Review")
public class PerformanceReviewController {

    private final PerformanceReviewService reviewService;

    @GetMapping
    public ApiResponse<List<PerformanceReviewResponse>> list(
            @RequestParam(required = false) String cycleId,
            @RequestParam(required = false) String personId) {
        return ApiResponse.ok(reviewService.list(cycleId, personId));
    }

    @PostMapping
    public ApiResponse<PerformanceReviewResponse> create(@RequestBody PerformanceReviewRequest req) {
        return ApiResponse.ok(reviewService.create(req));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<PerformanceReviewResponse> submit(@PathVariable String id) {
        return ApiResponse.ok(reviewService.submit(id));
    }

    @PostMapping("/{id}/manager-score")
    public ApiResponse<PerformanceReviewResponse> managerScore(
            @PathVariable String id, @RequestBody ManagerScoreRequest req) {
        return ApiResponse.ok(reviewService.managerScore(id, req));
    }
}
