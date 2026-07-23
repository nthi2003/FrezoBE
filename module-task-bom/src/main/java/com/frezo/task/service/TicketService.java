package com.frezo.task.service;

import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;

import java.util.List;

public interface TicketService {
    TicketResponse create(TicketRequest request);
    TicketResponse update(String id, TicketRequest request);
    void delete(String id) ;
    TicketResponse findById(String id);
    List<TicketResponse> findAll();
    TicketResponse updateStatus(String id, String status);
    TicketResponse assignTicket(String id, String assigneeId);
}
