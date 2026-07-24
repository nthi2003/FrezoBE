package com.frezo.task.service;

import com.frezo.task.dto.request.TicketCategoryRequest;
import com.frezo.task.dto.response.TicketCategoryResponse;

import java.util.List;

public interface TicketCategoryService {
    TicketCategoryResponse add(TicketCategoryRequest request);

    TicketCategoryResponse edit(String id, TicketCategoryRequest request);

    Void delete(String id);

    List<TicketCategoryResponse> findAll();

    List<TicketCategoryResponse> findActive();
}
