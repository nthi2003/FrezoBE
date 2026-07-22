package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.request.FacebookAccountRequest;
import com.frezo.fbautomation.dto.response.FacebookAccountResponse;
import com.frezo.fbautomation.service.FacebookAccountService;
import com.frezo.util.web.Response;
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
    public Response<List<FacebookAccountResponse>> getAll() {
        return Response.ok(accountService.getAll());
    }

    @Operation(summary = "Chi tiết tài khoản")
    @GetMapping("/{id}")
    public Response<FacebookAccountResponse> getById(@PathVariable String id) {
        return Response.ok(accountService.getById(id));
    }

    @Operation(summary = "Thêm tài khoản mới")
    @PostMapping
    public Response<FacebookAccountResponse> create(@Valid @RequestBody FacebookAccountRequest request) {
        return Response.ok(accountService.create(request));
    }

    @Operation(summary = "Cập nhật tài khoản")
    @PutMapping("/{id}")
    public Response<FacebookAccountResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody FacebookAccountRequest request) {
        return Response.ok(accountService.update(id, request));
    }

    @Operation(summary = "Xóa tài khoản")
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        return Response.ok();
    }

    @Operation(summary = "Cập nhật cookie")
    @PutMapping("/{id}/cookie")
    public Response<Void> updateCookie(@PathVariable String id, @RequestBody String cookie) {
        accountService.updateCookie(id, cookie);
        return Response.ok();
    }
}
