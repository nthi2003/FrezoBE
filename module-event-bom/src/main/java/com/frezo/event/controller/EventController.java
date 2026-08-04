package com.frezo.event.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.common.response.FePage;
import com.frezo.event.dto.request.EventSaveRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events — Admin", description = "CRUD sự kiện nội bộ + publish/cancel/calendar")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @CheckPermission(api = "/events", action = "VIEW")
    @Operation(summary = "Danh sách event (admin)")
    public ApiResponse<FePage<EventDto>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(eventService.listAdmin(status));
    }

    @GetMapping("/calendar")
    @CheckPermission(api = "/events/calendar", action = "VIEW")
    @Operation(summary = "Lịch sự kiện theo khoảng from/to")
    public ApiResponse<List<EventDto>> calendar(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.ok(eventService.calendar(from, to));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/events/{id}", action = "VIEW")
    public ApiResponse<EventDto> get(@PathVariable String id) {
        return ApiResponse.ok(eventService.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/events", action = "CREATE")
    public ApiResponse<EventDto> create(@RequestBody EventSaveRequest req) {
        return ApiResponse.ok(eventService.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/events/{id}", action = "UPDATE")
    public ApiResponse<EventDto> update(@PathVariable String id, @RequestBody EventSaveRequest req) {
        return ApiResponse.ok(eventService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/events/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        eventService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/publish")
    @CheckPermission(api = "/events/{id}/publish", action = "UPDATE")
    @Operation(summary = "Publish event → mở RSVP")
    public ApiResponse<EventDto> publish(@PathVariable String id) {
        return ApiResponse.ok(eventService.publish(id));
    }

    @PostMapping("/{id}/cancel")
    @CheckPermission(api = "/events/{id}/cancel", action = "UPDATE")
    @Operation(summary = "Huỷ event + notify người đã RSVP")
    public ApiResponse<EventDto> cancel(@PathVariable String id) {
        return ApiResponse.ok(eventService.cancel(id));
    }

    @GetMapping("/{id}/registrations")
    @CheckPermission(api = "/events/{id}/registrations", action = "VIEW")
    @Operation(summary = "Danh sách RSVP của event")
    public ApiResponse<List<EventRegistrationDto>> registrations(@PathVariable String id) {
        return ApiResponse.ok(eventService.listRegistrations(id));
    }
}
