package com.frezo.event.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.event.dto.request.RsvpRequest;
import com.frezo.event.dto.response.EventDto;
import com.frezo.event.dto.response.EventRegistrationDto;
import com.frezo.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events/portal")
@RequiredArgsConstructor
@Tag(name = "Events — Portal", description = "Portal đăng ký RSVP (user đã login)")
public class EventPortalController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Sự kiện PUBLISHED sắp diễn ra")
    public ApiResponse<FePage<EventDto>> list() {
        return ApiResponse.ok(eventService.listPortal());
    }

    @GetMapping("/my")
    @Operation(summary = "RSVP của tôi")
    public ApiResponse<List<EventRegistrationDto>> my() {
        return ApiResponse.ok(eventService.myRegistrations());
    }

    @GetMapping("/{id}")
    public ApiResponse<EventDto> get(@PathVariable String id) {
        return ApiResponse.ok(eventService.getPortal(id));
    }

    @PostMapping("/{id}/rsvp")
    @Operation(summary = "RSVP GOING|MAYBE|DECLINED — tôn trọng capacity")
    public ApiResponse<EventRegistrationDto> rsvp(@PathVariable String id,
                                                  @RequestBody(required = false) RsvpRequest req) {
        return ApiResponse.ok(eventService.rsvp(id, req != null ? req : new RsvpRequest()));
    }

    @DeleteMapping("/{id}/rsvp")
    @Operation(summary = "Huỷ RSVP")
    public ApiResponse<Void> cancelRsvp(@PathVariable String id) {
        eventService.cancelRsvp(id);
        return ApiResponse.ok();
    }
}
