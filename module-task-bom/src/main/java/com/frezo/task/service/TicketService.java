package com.frezo.task.service;

import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.util.web.Response;

import java.util.List;

public interface TicketService {
    Response<TicketResponse> create(TicketRequest request);
    Response<TicketResponse> update(String id, TicketRequest request);
    void delete(String id) ;
    Response<TicketResponse> findById(String id);
    Response<List<TicketResponse>> findAll();
    Response<TicketResponse> updateStatus(String id, String status);
    Response<TicketResponse> assignTicket(String id, String assigneeId);
}
