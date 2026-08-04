package com.frezo.task.controller;

import com.frezo.task.dto.request.ReviewRequest;
import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.service.TicketService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task/ticket")
@RequiredArgsConstructor
@Tag(name = "Ticket API", description = "Support Ticket Management APIs")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @CheckPermission(api = "/task/ticket", action = "CREATE")
    @Operation(summary = "Create a new ticket")
    public ApiResponse<TicketResponse> create(@RequestBody TicketRequest request) {
        return ApiResponse.ok(ticketService.create(request));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/task/ticket/{id}", action = "UPDATE")
    @Operation(summary = "Update an existing ticket")
    public ApiResponse<TicketResponse> update(@PathVariable String id, @RequestBody TicketRequest request) {
        return ApiResponse.ok(ticketService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/task/ticket/{id}", action = "DELETE")
    @Operation(summary = "Delete a ticket by ID")
    public  void delete(@PathVariable String id) {
         ticketService.delete(id);
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/task/ticket/{id}", action = "VIEW")
    @Operation(summary = "Get a ticket by ID")
    public ApiResponse<TicketResponse> findById(@PathVariable String id) {
        return ApiResponse.ok(ticketService.findById(id));
    }

    @GetMapping
    @CheckPermission(api = "/task/ticket", action = "VIEW")
    @Operation(summary = "Get all tickets")
    public ApiResponse<List<TicketResponse>> findAll() {
        return ApiResponse.ok(ticketService.findAll());
    }

    @PatchMapping("/{id}/assign/{assigneeId}")
    @CheckPermission(api = "/task/ticket/{id}/assign/{assigneeId}", action = "UPDATE")
    @Operation(summary = "Assign a ticket to a user")
    public ApiResponse<TicketResponse> assign(@PathVariable String id, @PathVariable String assigneeId) {
        return ApiResponse.ok(ticketService.assignTicket(id, assigneeId));
    }

    @PatchMapping("/{id}/status")
    @CheckPermission(api = "/task/ticket/{id}/status", action = "UPDATE")
    @Operation(summary = "Update ticket status")
    public ApiResponse<TicketResponse> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ApiResponse.ok(ticketService.updateStatus(id, status));
    }

    @PostMapping("/{id}/review")
    @CheckPermission(api = "/task/ticket/{id}/review", action = "UPDATE")
    @Operation(summary = "Manager/assigner review completed ticket (RESOLVED → CLOSED or back to IN_PROGRESS)")
    public ApiResponse<TicketResponse> review(
            @PathVariable String id,
            @RequestBody ReviewRequest request) {
        return ApiResponse.ok(ticketService.review(id, request));
    }
}
