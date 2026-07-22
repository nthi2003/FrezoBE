package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.crm.dto.MeetingRequest;
import com.frezo.crm.dto.MeetingResponse;
import com.frezo.crm.service.MeetingService;
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

import java.util.List;

@RestController
@RequestMapping("/crm/meetings")
@RequiredArgsConstructor
@Tag(name = "CRM — Meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    public ApiResponse<List<MeetingResponse>> list(@RequestParam(required = false) String dealId) {
        return ApiResponse.ok(meetingService.list(dealId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MeetingResponse> get(@PathVariable String id) {
        return ApiResponse.ok(meetingService.get(id));
    }

    @PostMapping
    public ApiResponse<MeetingResponse> create(@RequestBody MeetingRequest req) {
        return ApiResponse.ok(meetingService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<MeetingResponse> update(@PathVariable String id, @RequestBody MeetingRequest req) {
        return ApiResponse.ok(meetingService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        meetingService.delete(id);
        return ApiResponse.ok();
    }
}
