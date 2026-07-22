package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.entity.LeaveRecord;
import com.frezo.qlns.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/leave")
@RequiredArgsConstructor
@Tag(name = "Quản lý nghỉ phép", description = "API quản lý đơn xin nghỉ phép")
public class LeaveController {

    private final LeaveService leaveService;

    @Operation(summary = "Tạo đơn xin nghỉ phép")
    @PostMapping
    public ApiResponse<LeaveRecord> create(@RequestBody LeaveRecord request) {
        return ApiResponse.success(leaveService.create(request));
    }

    @Operation(summary = "Phê duyệt đơn nghỉ phép")
    @PutMapping("/{id}/approve")
    public ApiResponse<LeaveRecord> approve(@PathVariable String id, @RequestParam String managerId) {
        return ApiResponse.success(leaveService.approve(id, managerId));
    }

    @Operation(summary = "Lấy lịch sử nghỉ phép của nhân viên")
    @GetMapping("/person/{personId}")
    public ApiResponse<List<LeaveRecord>> getByPerson(@PathVariable String personId) {
        return ApiResponse.success(leaveService.getByPersonId(personId));
    }
}
