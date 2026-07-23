package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.request.FacebookAccountRequest;
import com.frezo.fbautomation.dto.response.FacebookAccountResponse;
import com.frezo.fbautomation.service.FacebookAccountService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fb/accounts")
@RequiredArgsConstructor
@Tag(name = "Facebook Accounts", description = "Quản lý tài khoản Facebook cho automation")
public class FacebookAccountController {

    private final FacebookAccountService accountService;

    @Operation(summary = "Danh sách tài khoản Facebook")
    @GetMapping
    public ApiResponse<List<FacebookAccountResponse>> getAll() {
        return ApiResponse.ok(accountService.getAll());
    }

    @Operation(summary = "Chi tiết tài khoản")
    @GetMapping("/{id}")
    public ApiResponse<FacebookAccountResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(accountService.getById(id));
    }

    @Operation(summary = "Thêm tài khoản mới")
    @PostMapping
    public ApiResponse<FacebookAccountResponse> create(@Valid @RequestBody FacebookAccountRequest request) {
        return ApiResponse.ok(accountService.create(request));
    }

    @Operation(summary = "Cập nhật tài khoản")
    @PutMapping("/{id}")
    public ApiResponse<FacebookAccountResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody FacebookAccountRequest request) {
        return ApiResponse.ok(accountService.update(id, request));
    }

    @Operation(summary = "Xóa tài khoản")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Cập nhật cookie")
    @PutMapping("/{id}/cookie")
    public ApiResponse<Void> updateCookie(@PathVariable String id, @RequestBody String cookie) {
        accountService.updateCookie(id, cookie);
        return ApiResponse.ok();
    }
}
