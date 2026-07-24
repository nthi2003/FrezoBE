package com.frezo.task.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.task.dto.request.TicketCategoryRequest;
import com.frezo.task.dto.response.TicketCategoryResponse;
import com.frezo.task.service.TicketCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task/ticket-category")
@RequiredArgsConstructor
@Tag(name = "Ticket Category API", description = "Master danh mục Ticket/Task")
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    @PostMapping
    @Operation(summary = "Thêm danh mục ticket")
    public ApiResponse<TicketCategoryResponse> add(@RequestBody TicketCategoryRequest request) {
        return ApiResponse.ok(ticketCategoryService.add(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa danh mục ticket")
    public ApiResponse<TicketCategoryResponse> edit(@PathVariable String id,
                                                    @RequestBody TicketCategoryRequest request) {
        return ApiResponse.ok(ticketCategoryService.edit(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ẩn/xoá mềm danh mục ticket")
    public ApiResponse<Void> delete(@PathVariable String id) {
        return ApiResponse.ok(ticketCategoryService.delete(id));
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả danh mục (admin)")
    public ApiResponse<List<TicketCategoryResponse>> findAll() {
        return ApiResponse.ok(ticketCategoryService.findAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Danh mục đang dùng — dropdown form ticket")
    public ApiResponse<List<TicketCategoryResponse>> findActive() {
        return ApiResponse.ok(ticketCategoryService.findActive());
    }
}
