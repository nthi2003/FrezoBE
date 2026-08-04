package com.frezo.qtht.controller;

import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.dto.request.PersonAddRequest;
import com.frezo.qtht.dto.request.PersonFilterRequest;
import com.frezo.qtht.dto.request.PersonUpdateRequest;
import com.frezo.qtht.service.PersonService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qlns/person")
@RequiredArgsConstructor
@Tag(name = "5. Quản lý Nhân viên (person)", description = "Các API quản lý thông tin nhân sự, tài khoản trong hệ thống")
public class PersonController {
    private final PersonService personService;

    @Operation(summary = "Danh sách người dùng (có lọc)", description = "Lấy danh sách người dùng dựa trên các tiêu chí lọc")
    @GetMapping("/all")
    @CheckPermission(api = "/qlns/person/all", action = "VIEW")
    public ApiResponse<?> all(@ModelAttribute PersonFilterRequest filter) {
        return ApiResponse.ok(personService.all(filter));
    }

    @Operation(summary = "Tạo mới người dùng", description = "Thêm một nhân sự mới vào hệ thống. Trong request body")
    @PostMapping("")
    @CheckPermission(api = "/qlns/person", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody PersonAddRequest apiRequest) {
        return personService.createPerson(apiRequest);
    }

    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin người dùng theo ID")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qlns/person/{id}", action = "UPDATE")
    public ApiResponse<?> update(
            @PathVariable("id") String id,
            @Valid @RequestBody PersonUpdateRequest request) {
        return personService.updatePerson(id, request);
    }

    @GetMapping("/combobox")
    @Operation(summary = "Combobox nhân sự — lookup dùng chung, JWT only")
    public ApiResponse<?> getCombobox(@ModelAttribute PersonFilterRequest filter) {
        return ApiResponse.ok(personService.getCombobox(filter));
    }

    @Operation(summary = "Lấy thông tin nhân viên theo ID")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/person/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable("id") String id) {
        return ApiResponse.ok(personService.getById(id));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "activate person", description = "Kích hoạt thông tin cá nhân ")
    @CheckPermission(api = "/qlns/person/{id}/activate", action = "UPDATE")
    public ApiResponse<?> activate(@Parameter(description = "person") @PathVariable String id) {
        personService.activate(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate person", description = "Vô hiệu hóa thông tin cá nhân ")
    @CheckPermission(api = "/qlns/person/{id}/deactivate", action = "UPDATE")
    public ApiResponse<?> deactivate(@Parameter(description = "person") @PathVariable String id) {
        personService.deactivate(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qlns/person/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable("id") String id) {
        personService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/upload-avatar-temp")
    @Operation(summary = "Upload temporary avatar", description = "Tải ảnh đại diện tạm thời lên MinIO, trả về URL để preview")
    @CheckPermission(api = "/qlns/person/upload-avatar-temp", action = "UPDATE")
    public ApiResponse<?> uploadAvatarTemp(
            @RequestParam("userName") String userName,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String tempUrl = personService.uploadAvatarTemp(userName, file);
        return ApiResponse.ok(tempUrl);
    }
}
