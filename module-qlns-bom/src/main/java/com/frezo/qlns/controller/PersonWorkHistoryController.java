package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.PersonWorkHistoryRequest;
import com.frezo.qlns.dto.response.PersonWorkHistoryResponse;
import com.frezo.qlns.service.PersonWorkHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/person-work-history")
@RequiredArgsConstructor
@Tag(name = "HR - Quá trình làm việc")
public class PersonWorkHistoryController {

    private final PersonWorkHistoryService personWorkHistoryService;

    @GetMapping
    @CheckPermission(api = "/qlns/person-work-history", action = "VIEW")
    public ApiResponse<List<PersonWorkHistoryResponse>> list(@RequestParam String personId) {
        return ApiResponse.ok(personWorkHistoryService.listByPerson(personId));
    }

    @PostMapping
    @CheckPermission(api = "/qlns/person-work-history", action = "CREATE")
    public ApiResponse<PersonWorkHistoryResponse> create(@RequestBody PersonWorkHistoryRequest request) {
        return ApiResponse.ok(personWorkHistoryService.create(request));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qlns/person-work-history/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        personWorkHistoryService.delete(id);
        return ApiResponse.ok();
    }
}
