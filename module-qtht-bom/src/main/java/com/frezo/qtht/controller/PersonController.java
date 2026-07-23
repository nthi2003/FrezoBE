package com.frezo.qtht.controller;

import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.dto.request.PersonAddRequest;
import com.frezo.qtht.dto.request.PersonFilterRequest;
import com.frezo.qtht.dto.request.PersonUpdateRequest;
import com.frezo.qtht.dto.response.PersonResponse;
import com.frezo.qtht.service.PersonService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qlns/person")
@RequiredArgsConstructor
@Tag(name = "5. Quản lý Nhân viên (person)", description = "Các API quản lý thông tin nhân sự, tài khoản trong hệ thống")
public class PersonController {
    private final PersonService personService;

    @Operation(summary = "Danh sách người dùng (có lọc)", description = "Lấy danh sách người dùng dựa trên các tiêu chí lọc")
    @GetMapping("/all")
//    @CheckPermission(api = "/qlns/person/all", action = "VIEW")
    public ApiResponse<?> all(@ModelAttribute PersonFilterRequest filter) {
        return ApiResponse.ok(personService.all(filter));
    }

    @Operation(summary = "Tạo mới người dùng", description = "Thêm một nhân sự mới vào hệ thống. Trong request body")
    @PostMapping("")
//    @CheckPermission(api = "/qlns/person", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody PersonAddRequest apiRequest) {
        return personService.createPerson(apiRequest);
    }

    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin người dùng theo ID")
    @PutMapping("/{id}")
//    @CheckPermission(api = "/qlns/person/{id}", action = "UPDATE")
    public ApiResponse<?> update(
            @PathVariable("id") String id,
            @Valid @RequestBody PersonUpdateRequest request) {
        return personService.updatePerson(id, request);
    }

    @GetMapping("/combobox")
    public ApiResponse<?> getCombobox(@ModelAttribute PersonFilterRequest filter) {
        Map<String, Object> data = personService.all(filter);
        Object itemsObj = data != null ? data.get("items") : null;

        if (!(itemsObj instanceof java.util.List<?> items)) {
            return ApiResponse.ok(java.util.List.of());
        }

        return ApiResponse.ok(items.stream()
                .filter(p -> p instanceof PersonResponse)
                .map(p -> (PersonResponse) p)
                .map(p -> ComboboxResponse.builder()
                        .value(p.getId())
                        .label(p.getName() + " (" + p.getCode() + ")")
                        .description(p.getJobTitle() + " - " + p.getEmail())
                        .build())
                .toList());
    }

    @Operation(summary = "Lấy thông tin nhân viên theo ID")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable("id") String id) {
        return ApiResponse.ok(personService.getById(id));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "activate person", description = "Kích hoạt thông tin cá nhân ")
    public ApiResponse<?> activate(@Parameter(description = "person") @PathVariable String id) {
        personService.activate(id);
        return ApiResponse.ok();
    }
    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate person", description = "Vô hiệu hóa thông tin cá nhân ")
    public ApiResponse<?> deactivate(@Parameter(description = "person") @PathVariable String id) {
        personService.deactivate(id);
        return ApiResponse.ok();
    }
    @DeleteMapping("/{id}")
//    @CheckPermission(api = "/qlns/person/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable("id") String id) {
        personService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/upload-avatar-temp")
    @Operation(summary = "Upload temporary avatar", description = "Tải ảnh đại diện tạm thời lên MinIO, trả về URL để preview")
    public ApiResponse<?> uploadAvatarTemp(
            @RequestParam("userName") String userName,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String tempUrl = personService.uploadAvatarTemp(userName, file);
        return ApiResponse.ok(tempUrl);
    }
}
