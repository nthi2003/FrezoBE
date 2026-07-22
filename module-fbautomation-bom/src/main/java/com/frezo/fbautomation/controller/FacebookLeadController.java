package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.response.FacebookLeadResponse;
import com.frezo.fbautomation.service.FacebookLeadService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fb/leads")
@RequiredArgsConstructor
@Tag(name = "Facebook Leads", description = "Khách hàng tiềm năng từ Facebook Groups")
public class FacebookLeadController {

    private final FacebookLeadService leadService;

    @Operation(summary = "Danh sách leads (lọc theo status + source)")
    @GetMapping
    public Response<List<FacebookLeadResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        return Response.ok(leadService.getAll(status, source));
    }

    @Operation(summary = "Assign lead cho nhân viên CSKH")
    @PostMapping("/{id}/assign")
    public Response<FacebookLeadResponse> assign(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return Response.error("username không được trống");
        }
        return Response.ok(leadService.assign(id, username));
    }

    @Operation(summary = "Chi tiết lead")
    @GetMapping("/{id}")
    public Response<FacebookLeadResponse> getById(@PathVariable String id) {
        return Response.ok(leadService.getById(id));
    }

    @Operation(summary = "Xóa lead")
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        leadService.delete(id);
        return Response.ok();
    }

    @Operation(summary = "Import lead vào danh sách khách hàng")
    @PostMapping("/{id}/import")
    public Response<String> importToCustomer(@PathVariable String id) {
        String customerId = leadService.importToCustomer(id);
        return Response.ok("Đã import thành công, customerId: " + customerId);
    }

    @Operation(summary = "Import nhiều leads vào danh sách khách hàng")
    @PostMapping("/import-batch")
    public Response<String> importBatch(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Response.error("Danh sách IDs không được trống");
        }
        int count = leadService.importAllToCustomer(ids);
        return Response.ok("Đã import thành công " + count + "/" + ids.size() + " khách hàng");
    }
}
