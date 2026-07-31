package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.OkrCheckInRequest;
import com.frezo.qlns.dto.request.OkrRequest;
import com.frezo.qlns.dto.response.OkrListResponse;
import com.frezo.qlns.dto.response.OkrResponse;
import com.frezo.qlns.service.OkrService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/qlns/okrs")
@RequiredArgsConstructor
@Tag(name = "QLNS — OKR")
public class OkrController {

    private final OkrService okrService;

    @GetMapping
    public ApiResponse<OkrListResponse> list(
            @RequestParam(required = false, defaultValue = "mine") String scope,
            @RequestParam(required = false) String ownerPersonId) {
        return ApiResponse.ok(okrService.list(scope, ownerPersonId));
    }

    @GetMapping("/{id}")
    public ApiResponse<OkrResponse> get(@PathVariable String id) {
        return ApiResponse.ok(okrService.get(id));
    }

    @PostMapping
    public ApiResponse<OkrResponse> create(@RequestBody OkrRequest req) {
        return ApiResponse.ok(okrService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<OkrResponse> update(@PathVariable String id, @RequestBody OkrRequest req) {
        return ApiResponse.ok(okrService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        okrService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<OkrResponse> checkIn(@PathVariable String id, @RequestBody OkrCheckInRequest req) {
        return ApiResponse.ok(okrService.checkIn(id, req));
    }
}
