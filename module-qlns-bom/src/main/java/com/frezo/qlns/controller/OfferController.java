package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.HireRequest;
import com.frezo.qlns.dto.request.OfferRequest;
import com.frezo.qlns.dto.response.OfferResponse;
import com.frezo.qlns.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qlns/recruitment/offers")
@RequiredArgsConstructor
@Tag(name = "Recruitment - Offer", description = "Thư mời & workflow phản hồi")
public class OfferController {

    private final OfferService offerService;

    @Operation(summary = "Tạo mới Offer (DRAFT)")
    @PostMapping
    public ApiResponse<OfferResponse> create(@RequestBody OfferRequest req) {
        return ApiResponse.ok(offerService.create(req));
    }

    @Operation(summary = "Gửi Offer tới ứng viên")
    @PostMapping("/{id}/send")
    public ApiResponse<OfferResponse> send(@PathVariable String id) {
        return ApiResponse.ok(offerService.send(id));
    }

    @Operation(summary = "Ứng viên chấp nhận Offer — auto HIRED (policy A: body User+Role)")
    @PostMapping("/{id}/accept")
    public ApiResponse<OfferResponse> accept(@PathVariable String id,
                                             @RequestBody(required = false) HireRequest hireRequest) {
        return ApiResponse.ok(offerService.accept(id, hireRequest));
    }

    @Operation(summary = "Ứng viên từ chối Offer")
    @PostMapping("/{id}/reject")
    public ApiResponse<OfferResponse> reject(@PathVariable String id) {
        return ApiResponse.ok(offerService.reject(id));
    }
}
