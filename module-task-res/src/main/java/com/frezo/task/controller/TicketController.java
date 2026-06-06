package com.frezo.task.controller;

import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.service.TicketService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket API", description = "Support Ticket Management APIs")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Create a new ticket")
    public Response<TicketResponse> create(@RequestBody TicketRequest request) {
        return ticketService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing ticket")
    public Response<TicketResponse> update(@PathVariable String id, @RequestBody TicketRequest request) {
        return ticketService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ticket by ID")
    public  void delete(@PathVariable String id) {
         ticketService.delete(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ticket by ID")
    public Response<TicketResponse> findById(@PathVariable String id) {
        return ticketService.findById(id);
    }

    @GetMapping
    @Operation(summary = "Get all tickets")
    public Response<List<TicketResponse>> findAll() {
        return ticketService.findAll();
    }

    @PatchMapping("/{id}/assign/{assigneeId}")
    @Operation(summary = "Assign a ticket to a user")
    public Response<TicketResponse> assign(@PathVariable String id, @PathVariable String assigneeId) {
        return ticketService.assignTicket(id, assigneeId);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ticket status")
    public Response<TicketResponse> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ticketService.updateStatus(id, status);
    }
}
