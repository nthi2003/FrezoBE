package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.CandidateRequest;
import com.frezo.qlns.dto.response.CandidateResponse;
import com.frezo.qlns.service.CandidateService;
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
@RequestMapping("/qlns/recruitment/candidates")
@RequiredArgsConstructor
@Tag(name = "Recruitment - Candidate", description = "Quản lý pool ứng viên")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Tạo mới ứng viên")
    @PostMapping
    @CheckPermission(api = "/qlns/recruitment/candidates", action = "CREATE")
    public ApiResponse<CandidateResponse> create(@RequestBody CandidateRequest req) {
        return ApiResponse.ok(candidateService.create(req));
    }

    @Operation(summary = "Tìm ứng viên theo keyword (name/email/phone)")
    @GetMapping
    @CheckPermission(api = "/qlns/recruitment/candidates", action = "VIEW")
    public ApiResponse<List<CandidateResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stage) {
        // Note: stage filter reserved cho phase sau — hiện tại chỉ search theo keyword.
        return ApiResponse.ok(candidateService.search(search));
    }

    @Operation(summary = "Chi tiết ứng viên")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/recruitment/candidates/{id}", action = "VIEW")
    public ApiResponse<CandidateResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(candidateService.getById(id));
    }
}
