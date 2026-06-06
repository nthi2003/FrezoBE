package com.frezo.task.service.impl;

import com.frezo.common.helper.SystemUtils;
import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.entity.Ticket;
import com.frezo.task.mapper.TicketMapper;
import com.frezo.task.repository.TicketRepository;
import com.frezo.task.service.TicketService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    @Override
    public Response<TicketResponse> create(TicketRequest request) {
        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setReporterId(SystemUtils.getCurrentUsername());
        ticket.setCode("TICKET-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        if (ticket.getStatus() == null) {
            ticket.setStatus(Ticket.TicketStatus.OPEN);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        return Response.ok(ticketMapper.toResponse(savedTicket));
    }

    @Override
    public Response<TicketResponse> update(String id, TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticketMapper.updateEntityFromRequest(request, ticket);

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        return Response.ok(ticketMapper.toResponse(updatedTicket));
    }

    @Override
    public void delete(String id) {
        if (!ticketRepository.existsById(id)) {
            throw new RuntimeException("Ticket not found");
        }
        ticketRepository.deleteById(id);

    }

    @Override
    public Response<TicketResponse> findById(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return Response.ok(ticketMapper.toResponse(ticket));
    }

    @Override
    public Response<List<TicketResponse>> findAll() {
        List<Ticket> tickets = ticketRepository.findAll();
        return Response.ok(ticketMapper.toResponseList(tickets));
    }

    @Override
    public Response<TicketResponse> updateStatus(String id, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        try {
            ticket.setStatus(Ticket.TicketStatus.valueOf(status.toUpperCase()));
            if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
            Ticket updatedTicket = ticketRepository.save(ticket);
            return Response.ok(ticketMapper.toResponse(updatedTicket));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid ticket status");
        }
    }

    @Override
    public Response<TicketResponse> assignTicket(String id, String assigneeId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAssigneeId(assigneeId);
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        Ticket updatedTicket = ticketRepository.save(ticket);
        return Response.ok(ticketMapper.toResponse(updatedTicket));
    }
}
